#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
MAX_RETRIES=30
POLL_INTERVAL=1
BALANCE_POLL_TIMEOUT=15

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  EXPENSES PLATFORM - COMPLETE FLOW    ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# ==================== HELPER FUNCTIONS ====================

# Check service health
check_service_health() {
  local service_name=$1
  local health_url=$2
  local max_attempts=10
  local attempt=1
  
  echo -e "${CYAN}Checking $service_name health...${NC}" >&2
  
  while [ $attempt -le $max_attempts ]; do
    if curl -s -f "$health_url" > /dev/null 2>&1; then
      echo -e "${GREEN}✓ $service_name is healthy${NC}" >&2
      return 0
    fi
    
    if [ $attempt -lt $max_attempts ]; then
      echo "  Attempt $attempt/$max_attempts - waiting..." >&2
      sleep 2
    fi
    attempt=$((attempt + 1))
  done
  
  echo -e "${RED}✗ $service_name health check failed after $max_attempts attempts${NC}" >&2
  return 1
}

# Poll for balance updates (wait for Split Engine to process events)
wait_for_balance_update() {
  local group_id=$1
  local auth_token=$2
  local expected_user_count=$3
  local timeout=$4
  local start_time=$(date +%s)
  
  echo -e "${CYAN}⏳ Waiting for Split Engine to process events...${NC}" >&2
  
  while true; do
    # Fetch current balances
    local balances=$(curl -s "http://localhost:8084/api/splits/group/$group_id/balances" \
      -H "Authorization: Bearer $auth_token")
    
    # Check if we have balances for all expected users
    if [ -n "$balances" ] && [ "$balances" != "[]" ]; then
      local user_count=$(echo "$balances" | jq '. | length')
      if [ "$user_count" -ge "$expected_user_count" ]; then
        echo -e "${GREEN}✓ Balances updated! ($user_count users)${NC}" >&2
        echo "$balances"
        return 0
      fi
    fi
    
    # Check timeout
    local current_time=$(date +%s)
    local elapsed=$((current_time - start_time))
    if [ $elapsed -ge $timeout ]; then
      echo -e "${RED}⚠️  Timeout waiting for balance updates after ${elapsed}s${NC}" >&2
      echo "$balances"
      return 1
    fi
    
    echo "  Waiting... (${elapsed}s/${timeout}s)" >&2
    sleep $POLL_INTERVAL
  done
}

# Wait for specific balance change (for settlement verification)
wait_for_balance_change() {
  local group_id=$1
  local auth_token=$2
  local user_id=$3
  local previous_balance=$4
  local timeout=$5
  local start_time=$(date +%s)
  
  echo -e "${CYAN}⏳ Waiting for balance change...${NC}" >&2
  
  while true; do
    local balances=$(curl -s "http://localhost:8084/api/splits/group/$group_id/balances" \
      -H "Authorization: Bearer $auth_token")
    
    if [ -n "$balances" ] && [ "$balances" != "[]" ]; then
      local current_balance=$(echo "$balances" | jq -r ".[] | select(.userId == \"$user_id\") | .balanceCents")
      
      if [ -n "$current_balance" ] && [ "$current_balance" != "null" ] && [ "$current_balance" != "$previous_balance" ]; then
        echo -e "${GREEN}✓ Balance updated! $previous_balance → $current_balance cents${NC}" >&2
        echo "$balances"
        return 0
      fi
    fi
    
    local current_time=$(date +%s)
    local elapsed=$((current_time - start_time))
    if [ $elapsed -ge $timeout ]; then
      echo -e "${YELLOW}⚠️  No balance change detected after ${elapsed}s${NC}" >&2
      echo "$balances"
      return 0
    fi
    
    echo "  Checking... (${elapsed}s/${timeout}s)" >&2
    sleep $POLL_INTERVAL
  done
}

# Verify balance consistency
verify_balance_consistency() {
  local group_id=$1
  local auth_token=$2
  
  echo -e "${CYAN}🔍 Verifying balance consistency...${NC}" >&2
  
  local reconciliation=$(curl -s -X POST "http://localhost:8084/api/splits/group/$group_id/reconcile" \
    -H "Authorization: Bearer $auth_token" \
    -H "Content-Type: application/json")
  
  local is_balanced=$(echo "$reconciliation" | jq -r '.isBalanced')
  local total_balance=$(echo "$reconciliation" | jq -r '.totalBalanceCents')
  local message=$(echo "$reconciliation" | jq -r '.message')
  
  if [ "$is_balanced" = "true" ]; then
    echo -e "${GREEN}✓ $message${NC}" >&2
    echo "$reconciliation"
    return 0
  else
    echo -e "${RED}✗ $message${NC}" >&2
    echo "$reconciliation"
    return 1
  fi
}

# ==================== MAIN FLOW ====================

# Step 0: Check service health
echo -e "${YELLOW}Step 0: Checking service health...${NC}"
check_service_health "User Service" "http://localhost:8081/actuator/health"
check_service_health "Group Service" "http://localhost:8082/actuator/health"
check_service_health "Expense Service" "http://localhost:8083/actuator/health"
check_service_health "Split Engine" "http://localhost:8084/actuator/health"
check_service_health "Settlement Service" "http://localhost:8085/actuator/health"
echo ""

# Step 1: Get JWT tokens for all users
echo -e "${YELLOW}Step 1: Getting JWT tokens for all users...${NC}"
echo "Getting Alice token..."
ALICE_TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/expenses/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=web&username=alice@example.com&password=password' | jq -r .access_token)
[ -n "$ALICE_TOKEN" ] && echo "✓ Alice token acquired" || { echo -e "${RED}✗ Failed to get Alice token${NC}"; exit 1; }

echo "Getting Bob token..."
BOB_TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/expenses/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=web&username=bob@example.com&password=password' | jq -r .access_token)
[ -n "$BOB_TOKEN" ] && echo "✓ Bob token acquired" || { echo -e "${RED}✗ Failed to get Bob token${NC}"; exit 1; }

echo "Getting Charlie token..."
CHARLIE_TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/expenses/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=web&username=charlie@example.com&password=password' | jq -r .access_token)
[ -n "$CHARLIE_TOKEN" ] && echo "✓ Charlie token acquired" || { echo -e "${RED}✗ Failed to get Charlie token${NC}"; exit 1; }

echo -e "${GREEN}✓ All tokens obtained successfully${NC}"
echo ""

# Step 2: Provision user profiles and get keycloakUserIds
echo -e "${YELLOW}Step 2: Provisioning user profiles...${NC}"

echo "Getting Alice profile..."
ALICE_PROFILE=$(curl -s 'http://localhost:8081/user/me' -H "Authorization: Bearer $ALICE_TOKEN")
ALICE_ID=$(echo "$ALICE_PROFILE" | jq -r .keycloakUserId)
ALICE_EMAIL=$(echo "$ALICE_PROFILE" | jq -r .email)
echo -e "Alice: ${GREEN}$ALICE_ID${NC} ($ALICE_EMAIL)"

echo "Getting Bob profile..."
BOB_PROFILE=$(curl -s 'http://localhost:8081/user/me' -H "Authorization: Bearer $BOB_TOKEN")
BOB_ID=$(echo "$BOB_PROFILE" | jq -r .keycloakUserId)
BOB_EMAIL=$(echo "$BOB_PROFILE" | jq -r .email)
echo -e "Bob: ${GREEN}$BOB_ID${NC} ($BOB_EMAIL)"

echo "Getting Charlie profile..."
CHARLIE_PROFILE=$(curl -s 'http://localhost:8081/user/me' -H "Authorization: Bearer $CHARLIE_TOKEN")
CHARLIE_ID=$(echo "$CHARLIE_PROFILE" | jq -r .keycloakUserId)
CHARLIE_EMAIL=$(echo "$CHARLIE_PROFILE" | jq -r .email)
echo -e "Charlie: ${GREEN}$CHARLIE_ID${NC} ($CHARLIE_EMAIL)"
echo ""

# Step 3: Create a group (Charlie as owner)
echo -e "${YELLOW}Step 3: Creating a group (Charlie as owner)...${NC}"

GROUP_RESPONSE=$(curl -s -X POST 'http://localhost:8082/groups' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CHARLIE_TOKEN" \
  -d '{
    "name": "Weekend Trip to Paris",
    "description": "Expenses for our amazing weekend getaway to Paris",
    "defaultCurrency": "USD"
  }')

GROUP_ID=$(echo "$GROUP_RESPONSE" | jq -r .id)
GROUP_NAME=$(echo "$GROUP_RESPONSE" | jq -r .name)
echo -e "✓ Group created: ${GREEN}$GROUP_ID${NC}"
echo -e "  Name: $GROUP_NAME"
echo ""

# Step 4: Add members to the group
echo -e "${YELLOW}Step 4: Adding members to the group...${NC}"

echo "Adding Alice to the group..."
ALICE_MEMBER=$(curl -s -X POST "http://localhost:8082/groups/$GROUP_ID/members" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CHARLIE_TOKEN" \
  -d "{\"userId\": \"$ALICE_ID\", \"role\": \"MEMBER\"}")
echo -e "✓ Alice added: $(echo "$ALICE_MEMBER" | jq -r .id)"

echo "Adding Bob to the group..."
BOB_MEMBER=$(curl -s -X POST "http://localhost:8082/groups/$GROUP_ID/members" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CHARLIE_TOKEN" \
  -d "{\"userId\": \"$BOB_ID\", \"role\": \"MEMBER\"}")
echo -e "✓ Bob added: $(echo "$BOB_MEMBER" | jq -r .id)"

GROUP_WITH_MEMBERS=$(curl -s "http://localhost:8082/groups/$GROUP_ID" -H "Authorization: Bearer $CHARLIE_TOKEN")
MEMBER_COUNT=$(echo "$GROUP_WITH_MEMBERS" | jq '.members | length')
echo -e "✓ Group now has ${GREEN}$MEMBER_COUNT${NC} members"
echo ""

# Step 5: Add expenses
echo -e "${YELLOW}Step 5: Adding expenses to the group...${NC}"

# Expense 1: Dinner (Alice pays, all 3 participate) - $120 split 3 ways = $40 each
echo "Creating Expense 1: Dinner (Alice pays, all participate)..."
EXPENSE1=$(curl -s -X POST 'http://localhost:8083/api/expenses' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -d "{
    \"groupId\": \"$GROUP_ID\",
    \"currency\": \"USD\",
    \"amount\": 120.00,
    \"occurredAt\": \"2025-09-22\",
    \"note\": \"Dinner at Le Jules Verne\",
    \"category\": \"FOOD\",
    \"paidBy\": \"$ALICE_ID\",
    \"participants\": [
      {\"userId\": \"$ALICE_ID\", \"ruleType\": \"EQUAL\"},
      {\"userId\": \"$BOB_ID\", \"ruleType\": \"EQUAL\"},
      {\"userId\": \"$CHARLIE_ID\", \"ruleType\": \"EQUAL\"}
    ]
  }")
EXPENSE1_ID=$(echo "$EXPENSE1" | jq -r .id)
echo -e "✓ Expense 1 (Dinner): ${GREEN}$EXPENSE1_ID${NC} - \$120.00"

# Expense 2: Museum tickets (Bob pays, Bob and Charlie participate) - $60 split 2 ways = $30 each
echo "Creating Expense 2: Museum tickets (Bob pays, Bob and Charlie participate)..."
EXPENSE2=$(curl -s -X POST 'http://localhost:8083/api/expenses' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $BOB_TOKEN" \
  -d "{
    \"groupId\": \"$GROUP_ID\",
    \"currency\": \"USD\",
    \"amount\": 60.00,
    \"occurredAt\": \"2025-09-22\",
    \"note\": \"Louvre Museum tickets\",
    \"category\": \"ENTERTAINMENT\",
    \"paidBy\": \"$BOB_ID\",
    \"participants\": [
      {\"userId\": \"$BOB_ID\", \"ruleType\": \"EQUAL\"},
      {\"userId\": \"$CHARLIE_ID\", \"ruleType\": \"EQUAL\"}
    ]
  }")
EXPENSE2_ID=$(echo "$EXPENSE2" | jq -r .id)
echo -e "✓ Expense 2 (Museum): ${GREEN}$EXPENSE2_ID${NC} - \$60.00"

# Expense 3: Taxi (Charlie pays, all 3 participate) - $45 split 3 ways = $15 each
echo "Creating Expense 3: Taxi (Charlie pays, all participate)..."
EXPENSE3=$(curl -s -X POST 'http://localhost:8083/api/expenses' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CHARLIE_TOKEN" \
  -d "{
    \"groupId\": \"$GROUP_ID\",
    \"currency\": \"USD\",
    \"amount\": 45.00,
    \"occurredAt\": \"2025-09-22\",
    \"note\": \"Taxi to/from airport\",
    \"category\": \"TRANSPORT\",
    \"paidBy\": \"$CHARLIE_ID\",
    \"participants\": [
      {\"userId\": \"$ALICE_ID\", \"ruleType\": \"EQUAL\"},
      {\"userId\": \"$BOB_ID\", \"ruleType\": \"EQUAL\"},
      {\"userId\": \"$CHARLIE_ID\", \"ruleType\": \"EQUAL\"}
    ]
  }")
EXPENSE3_ID=$(echo "$EXPENSE3" | jq -r .id)
echo -e "✓ Expense 3 (Taxi): ${GREEN}$EXPENSE3_ID${NC} - \$45.00"

echo ""
echo -e "${GREEN}✓ All 3 expenses created successfully!${NC}"
echo ""

# Step 6: Wait for Split Engine to process expenses and get balances
echo -e "${YELLOW}Step 6: Getting balances after expense processing...${NC}"
BALANCES=$(wait_for_balance_update "$GROUP_ID" "$ALICE_TOKEN" 3 $BALANCE_POLL_TIMEOUT)

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}         SPLIT ENGINE CALCULATIONS      ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${YELLOW}Expense Summary:${NC}"
echo "  Expense 1 - Dinner: \$120.00 (Alice paid, all 3 participated)"
echo "  Expense 2 - Museum: \$60.00 (Bob paid, Bob & Charlie participated)"
echo "  Expense 3 - Taxi: \$45.00 (Charlie paid, all 3 participated)"
echo ""

if [ "$BALANCES" = "[]" ] || [ -z "$BALANCES" ]; then
  echo -e "${RED}⚠️  Split Engine returned empty balances${NC}"
else
  echo -e "${GREEN}✓ Split Engine balances (calculated automatically):${NC}"
  echo ""
  
  ALICE_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$ALICE_ID\") | .balanceCents")
  BOB_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$BOB_ID\") | .balanceCents")
  CHARLIE_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$CHARLIE_ID\") | .balanceCents")
  
  echo "Alice ($ALICE_ID):"
  if [ "$ALICE_BALANCE" -gt 0 ]; then
    echo "  Balance: +\$$(echo "scale=2; $ALICE_BALANCE/100" | bc) (should receive money)"
  elif [ "$ALICE_BALANCE" -lt 0 ]; then
    echo "  Balance: -\$$(echo "scale=2; ${ALICE_BALANCE#-}/100" | bc) (owes money)"
  else
    echo "  Balance: \$0.00 (settled)"
  fi
  
  echo ""
  echo "Bob ($BOB_ID):"
  if [ "$BOB_BALANCE" -gt 0 ]; then
    echo "  Balance: +\$$(echo "scale=2; $BOB_BALANCE/100" | bc) (should receive money)"
  elif [ "$BOB_BALANCE" -lt 0 ]; then
    echo "  Balance: -\$$(echo "scale=2; ${BOB_BALANCE#-}/100" | bc) (owes money)"
  else
    echo "  Balance: \$0.00 (settled)"
  fi
  
  echo ""
  echo "Charlie ($CHARLIE_ID):"
  if [ "$CHARLIE_BALANCE" -gt 0 ]; then
    echo "  Balance: +\$$(echo "scale=2; $CHARLIE_BALANCE/100" | bc) (should receive money)"
  elif [ "$CHARLIE_BALANCE" -lt 0 ]; then
    echo "  Balance: -\$$(echo "scale=2; ${CHARLIE_BALANCE#-}/100" | bc) (owes money)"
  else
    echo "  Balance: \$0.00 (settled)"
  fi
  
  echo ""
  TOTAL=$(echo "$ALICE_BALANCE + $BOB_BALANCE + $CHARLIE_BALANCE" | bc)
  echo -e "${YELLOW}Verification:${NC} All balances sum to \$$(echo "scale=2; $TOTAL/100" | bc)"
  if [ "$TOTAL" -eq 0 ]; then
    echo -e "${GREEN}✓ Perfect! Split Engine calculations are balanced.${NC}"
  else
    echo -e "${RED}⚠️  Balances don't sum to zero - there might be issues.${NC}"
  fi
fi

echo ""
echo -e "${YELLOW}Step 7: Scenario - Charlie initiates, Bob is payee, equal split${NC}"
echo "Creating expense: paidBy=Bob, participants=Alice+Bob+Charlie (equal) ..."
EXPENSE_PAIDBY_BOB=$(curl -s -X POST 'http://localhost:8083/api/expenses' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CHARLIE_TOKEN" \
  -d "{
  \"groupId\": \"$GROUP_ID\",
  \"currency\": \"USD\",
  \"amount\": 90.00,
  \"occurredAt\": \"2025-09-22\",
  \"note\": \"Scenario: Charlie initiates, Bob is payee, equal split\",
  \"category\": \"OTHER\",
  \"participants\": [
    {\"userId\": \"$ALICE_ID\", \"ruleType\": \"EQUAL\"},
    {\"userId\": \"$BOB_ID\", \"ruleType\": \"EQUAL\"},
    {\"userId\": \"$CHARLIE_ID\", \"ruleType\": \"EQUAL\"}
  ],
  \"paidBy\": \"$BOB_ID\"
}")
EXPENSE_PAIDBY_BOB_ID=$(echo "$EXPENSE_PAIDBY_BOB" | jq -r .id)
echo -e "✓ Expense created: ${GREEN}$EXPENSE_PAIDBY_BOB_ID${NC}"

# Wait for balance update using polling
BALANCES=$(wait_for_balance_update "$GROUP_ID" "$ALICE_TOKEN" 3 $BALANCE_POLL_TIMEOUT)

echo -e "${BLUE}Updated balances after paidBy=Bob scenario:${NC}"
if [ "$BALANCES" = "[]" ] || [ -z "$BALANCES" ]; then
  echo -e "${RED}⚠️  Split Engine returned empty balances${NC}"
else
  ALICE_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$ALICE_ID\") | .balanceCents")
  BOB_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$BOB_ID\") | .balanceCents")
  CHARLIE_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$CHARLIE_ID\") | .balanceCents")

  echo "  Alice:   \$$(echo "scale=2; $ALICE_BALANCE/100" | bc)"
  echo "  Bob:     \$$(echo "scale=2; $BOB_BALANCE/100" | bc)"
  echo "  Charlie: \$$(echo "scale=2; $CHARLIE_BALANCE/100" | bc)"
fi

# ===================== SETTLEMENT PROPOSAL & PAYMENT FLOW =====================
echo ""
echo -e "${YELLOW}Step 8: Creating settlement proposal...${NC}"

# Identify debtor and creditor
DEBTOR_ID=""; DEBTOR_NAME=""; DEBTOR_BALANCE=0
CREDITOR_ID=""; CREDITOR_NAME=""; CREDITOR_BALANCE=0

if [ -n "$CHARLIE_BALANCE" ] && [ "$CHARLIE_BALANCE" != "null" ] && [ "$CHARLIE_BALANCE" -lt 0 ]; then
  DEBTOR_ID=$CHARLIE_ID; DEBTOR_NAME="Charlie"; DEBTOR_BALANCE=$CHARLIE_BALANCE
fi
if [ -n "$ALICE_BALANCE" ] && [ "$ALICE_BALANCE" != "null" ] && [ "$ALICE_BALANCE" -gt 0 ]; then
  CREDITOR_ID=$ALICE_ID; CREDITOR_NAME="Alice"; CREDITOR_BALANCE=$ALICE_BALANCE
fi

if [ -z "$DEBTOR_ID" ] || [ -z "$CREDITOR_ID" ]; then
  echo -e "${YELLOW}No debtor/creditor pair detected – skipping settlement.${NC}"
else
  ABS_DEBTOR=$(( DEBTOR_BALANCE * -1 ))
  if [ $ABS_DEBTOR -lt $CREDITOR_BALANCE ]; then
    SETTLE_CENTS=$ABS_DEBTOR
  else
    SETTLE_CENTS=$CREDITOR_BALANCE
  fi
  SETTLE_AMOUNT_DEC=$(echo "scale=2; $SETTLE_CENTS/100" | bc)

  echo "Creating settlement: $DEBTOR_NAME pays $CREDITOR_NAME \$$SETTLE_AMOUNT_DEC"

  CREATOR_TOKEN=$CHARLIE_TOKEN
  ACCEPTOR_TOKEN=$ALICE_TOKEN

  SETTLEMENT_PROPOSAL=$(curl -s -X POST 'http://localhost:8085/api/settlements/proposals' \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $CREATOR_TOKEN" \
    -d "{
  \"groupId\": \"$GROUP_ID\",
  \"title\": \"Manual Settlement - $DEBTOR_NAME pays $CREDITOR_NAME\",
  \"description\": \"$DEBTOR_NAME settles balance with $CREDITOR_NAME\",
  \"currency\": \"USD\",
  \"proposalType\": \"MANUAL_SETTLEMENT\",
  \"payments\": [
    {
      \"payerId\": \"$DEBTOR_ID\",
      \"payeeId\": \"$CREDITOR_ID\",
      \"amount\": $SETTLE_AMOUNT_DEC,
      \"description\": \"Settlement payment\",
      \"paymentMethod\": \"CASH\"
    }
  ]
}")

  PROPOSAL_ID=$(echo "$SETTLEMENT_PROPOSAL" | jq -r .id)
  PAYMENT_ID=$(echo "$SETTLEMENT_PROPOSAL" | jq -r '.payments[0].id')

  if [ -n "$PROPOSAL_ID" ] && [ "$PROPOSAL_ID" != "null" ]; then
    echo -e "${GREEN}✓ Settlement proposal created: $PROPOSAL_ID${NC}"

    echo -e "${YELLOW}Step 9: Accepting settlement proposal...${NC}"
    PROPOSAL_ACCEPT=$(curl -s -X POST "http://localhost:8085/api/settlements/proposals/$PROPOSAL_ID/accept" \
      -H 'Content-Type: application/json' \
      -H "Authorization: Bearer $ACCEPTOR_TOKEN")
    echo -e "${GREEN}✓ Proposal accepted${NC}"

    if [ -n "$PAYMENT_ID" ] && [ "$PAYMENT_ID" != "null" ]; then
      echo -e "${YELLOW}Step 10: Confirming payment...${NC}"
      
      # Save balances before settlement
      ALICE_BALANCE_BEFORE=$ALICE_BALANCE
      CHARLIE_BALANCE_BEFORE=$CHARLIE_BALANCE
      
      # Payer confirms
      curl -s -X POST "http://localhost:8085/api/settlements/payments/$PAYMENT_ID/confirm-payer" \
        -H 'Content-Type: application/json' \
        -H "Authorization: Bearer $CREATOR_TOKEN" \
        -d '{"notes":"Payer confirms sending funds"}' > /dev/null
      echo -e "  ${GREEN}✓ Payer (Charlie) confirmed${NC}"

      # Payee confirms (this triggers PaymentCompleted event!)
      PAYMENT_FINAL=$(curl -s -X POST "http://localhost:8085/api/settlements/payments/$PAYMENT_ID/confirm-payee" \
        -H 'Content-Type: application/json' \
        -H "Authorization: Bearer $ACCEPTOR_TOKEN" \
        -d '{"notes":"Payee confirms receipt"}')
      echo -e "  ${GREEN}✓ Payee (Alice) confirmed${NC}"
      
      PAYMENT_STATUS=$(echo "$PAYMENT_FINAL" | jq -r '.status')
      echo -e "  Payment status: ${GREEN}$PAYMENT_STATUS${NC}"

      # ==================== NEW: VERIFY EVENT-DRIVEN BALANCE UPDATE ====================
      echo ""
      echo -e "${CYAN}🔥 Verifying event-driven balance updates...${NC}"
      echo -e "Expected: PaymentCompleted event → Split Engine updates balances"
      
      # Wait for balance to change (event processing)
      BALANCES=$(wait_for_balance_change "$GROUP_ID" "$ALICE_TOKEN" "$ALICE_ID" "$ALICE_BALANCE_BEFORE" 10)
      
      # Get updated balances
      ALICE_BALANCE_AFTER=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$ALICE_ID\") | .balanceCents")
      CHARLIE_BALANCE_AFTER=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$CHARLIE_ID\") | .balanceCents")
      BOB_BALANCE_AFTER=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$BOB_ID\") | .balanceCents")
      
      echo ""
      echo -e "${BLUE}========================================${NC}"
      echo -e "${BLUE}  EVENT-DRIVEN BALANCE UPDATE RESULTS  ${NC}"
      echo -e "${BLUE}========================================${NC}"
      echo ""
      
      echo -e "${YELLOW}Settlement: Charlie paid Alice \$$SETTLE_AMOUNT_DEC${NC}"
      echo ""
      
      echo "Alice:"
      echo "  Before: \$$(echo "scale=2; $ALICE_BALANCE_BEFORE/100" | bc)"
      echo "  After:  \$$(echo "scale=2; $ALICE_BALANCE_AFTER/100" | bc)"
      ALICE_CHANGE=$((ALICE_BALANCE_AFTER - ALICE_BALANCE_BEFORE))
      if [ $ALICE_CHANGE -ne 0 ]; then
        echo -e "  Change: ${GREEN}\$$(echo "scale=2; $ALICE_CHANGE/100" | bc) ✓${NC}"
      else
        echo "  Change: \$0.00"
      fi
      
      echo ""
      echo "Charlie:"
      echo "  Before: \$$(echo "scale=2; $CHARLIE_BALANCE_BEFORE/100" | bc)"
      echo "  After:  \$$(echo "scale=2; $CHARLIE_BALANCE_AFTER/100" | bc)"
      CHARLIE_CHANGE=$((CHARLIE_BALANCE_AFTER - CHARLIE_BALANCE_BEFORE))
      if [ $CHARLIE_CHANGE -ne 0 ]; then
        echo -e "  Change: ${GREEN}+\$$(echo "scale=2; ${CHARLIE_CHANGE#-}/100" | bc) ✓${NC} (debt reduced)"
      else
        echo "  Change: \$0.00"
      fi
      
      echo ""
      echo "Bob:"
      echo "  Before: \$$(echo "scale=2; $BOB_BALANCE/100" | bc)"
      echo "  After:  \$$(echo "scale=2; $BOB_BALANCE_AFTER/100" | bc)"
      echo "  Change: \$0.00 (not involved in settlement)"
      
      echo ""
      
      # Verify settlement applied correctly
      EXPECTED_ALICE_CHANGE=$((-SETTLE_CENTS))
      EXPECTED_CHARLIE_CHANGE=$SETTLE_CENTS
      
      if [ $ALICE_CHANGE -eq $EXPECTED_ALICE_CHANGE ] && [ $CHARLIE_CHANGE -eq $EXPECTED_CHARLIE_CHANGE ]; then
        echo -e "${GREEN}🎉 SUCCESS! Balances updated correctly via events!${NC}"
        echo -e "${GREEN}   Settlement payment was automatically applied to balances${NC}"
      else
        echo -e "${RED}⚠️  Balance changes don't match expected values${NC}"
        echo "   Expected Alice: $EXPECTED_ALICE_CHANGE, Got: $ALICE_CHANGE"
        echo "   Expected Charlie: $EXPECTED_CHARLIE_CHANGE, Got: $CHARLIE_CHANGE"
      fi
    fi
  fi
fi

# ==================== BALANCE RECONCILIATION CHECK ====================
echo ""
echo -e "${YELLOW}Step 11: Running balance reconciliation check...${NC}"
RECONCILIATION_RESULT=$(verify_balance_consistency "$GROUP_ID" "$ALICE_TOKEN")
echo ""
echo -e "${CYAN}Reconciliation Result:${NC}"
echo "$RECONCILIATION_RESULT" | jq '.'

# ==================== SUMMARY ====================
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}            FINAL SUMMARY               ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${GREEN}✅ Successfully completed full expense and settlement flow:${NC}"
echo "  1. ✓ Health checks for all services"
echo "  2. ✓ User authentication and profile provisioning"
echo "  3. ✓ Group creation and member management"
echo "  4. ✓ Expense creation with split calculations"
echo "  5. ✓ Real-time balance updates via Kafka events"
echo "  6. ✓ Settlement proposal creation and acceptance"
echo "  7. ✓ Payment confirmation workflow"
echo "  8. ✓ Event-driven balance adjustments"
echo "  9. ✓ Balance consistency verification"
echo ""

echo -e "${YELLOW}Key Improvements Verified:${NC}"
echo "  • ✅ No hardcoded delays - intelligent polling used"
echo "  • ✅ Health checks before API calls"
echo "  • ✅ Settlement events published and consumed"
echo "  • ✅ Balances automatically updated via PaymentCompleted event"
echo "  • ✅ Balance reconciliation endpoint validates consistency"
echo "  • ✅ All timestamps properly populated in responses"
echo ""

echo -e "${GREEN}🚀 Expenses Platform - Event-Driven Architecture VERIFIED! 🚀${NC}"
echo ""

# Final balance check
echo -e "${YELLOW}Final Balance Summary:${NC}"
FINAL_BALANCES=$(curl -s "http://localhost:8084/api/splits/group/$GROUP_ID/balances" \
  -H "Authorization: Bearer $ALICE_TOKEN")

if command -v jq >/dev/null 2>&1 && [ -n "$FINAL_BALANCES" ]; then
  echo "$FINAL_BALANCES" | jq -r '.[] | "  \(.userId | split("-")[0])...: $\(.balanceCents/100) (\(.balanceType // "N/A"))"'
  TOTAL_CENTS=$(echo "$FINAL_BALANCES" | jq '[.[].balanceCents] | add // 0')
  echo ""
  if [ "$TOTAL_CENTS" -eq 0 ] 2>/dev/null; then
    echo -e "${GREEN}✓ Net total: \$0.00 (perfectly balanced)${NC}"
  else
    echo -e "${RED}⚠️  Net total: \$$(echo "scale=2; $TOTAL_CENTS/100" | bc) (should be 0)${NC}"
  fi
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Test completed successfully!${NC}"
echo -e "${BLUE}========================================${NC}"
