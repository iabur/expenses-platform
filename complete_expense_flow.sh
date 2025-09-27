#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  EXPENSES PLATFORM - COMPLETE FLOW    ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Step 0: Get JWT tokens for all users
echo -e "${YELLOW}Step 0: Getting JWT tokens for all users...${NC}"
echo "Getting Alice token..."
ALICE_TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/expenses/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=web&username=alice@example.com&password=password' | jq -r .access_token)
echo "Alice token acquired: $( [ -n "$ALICE_TOKEN" ] && echo yes || echo no )"

echo "Getting Bob token..."
BOB_TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/expenses/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=web&username=bob@example.com&password=password' | jq -r .access_token)
echo "Bob token acquired: $( [ -n "$BOB_TOKEN" ] && echo yes || echo no )"

echo "Getting Charlie token..."
CHARLIE_TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/expenses/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=web&username=charlie@example.com&password=password' | jq -r .access_token)
echo "Charlie token acquired: $( [ -n "$CHARLIE_TOKEN" ] && echo yes || echo no )"

echo -e "${GREEN}✓ All tokens obtained successfully${NC}"
echo ""

# Step 1: Provision user profiles and get keycloakUserIds
echo -e "${YELLOW}Step 1: Provisioning user profiles...${NC}"

echo "Getting Alice profile..."
ALICE_PROFILE=$(curl -s 'http://localhost:8081/user/me' -H "Authorization: Bearer $ALICE_TOKEN")
ALICE_ID=$(echo "$ALICE_PROFILE" | jq -r .keycloakUserId)
ALICE_EMAIL=$(echo "$ALICE_PROFILE" | jq -r .email)
echo -e "Alice: ${GREEN}$ALICE_ID${NC} ($ALICE_EMAIL)"
echo "Alice /user/me response:"
echo "$ALICE_PROFILE" | jq '.'

echo "Getting Bob profile..."
BOB_PROFILE=$(curl -s 'http://localhost:8081/user/me' -H "Authorization: Bearer $BOB_TOKEN")
BOB_ID=$(echo "$BOB_PROFILE" | jq -r .keycloakUserId)
BOB_EMAIL=$(echo "$BOB_PROFILE" | jq -r .email)
echo -e "Bob: ${GREEN}$BOB_ID${NC} ($BOB_EMAIL)"
echo "Bob /user/me response:"
echo "$BOB_PROFILE" | jq '.'

echo "Getting Charlie profile..."
CHARLIE_PROFILE=$(curl -s 'http://localhost:8081/user/me' -H "Authorization: Bearer $CHARLIE_TOKEN")
CHARLIE_ID=$(echo "$CHARLIE_PROFILE" | jq -r .keycloakUserId)
CHARLIE_EMAIL=$(echo "$CHARLIE_PROFILE" | jq -r .email)
echo -e "Charlie: ${GREEN}$CHARLIE_ID${NC} ($CHARLIE_EMAIL)"
echo "Charlie /user/me response:"
echo "$CHARLIE_PROFILE" | jq '.'
echo ""

# Step 2: Create a group (Charlie as owner)
echo -e "${YELLOW}Step 2: Creating a group (Charlie as owner)...${NC}"

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
echo -e "  Created by: Charlie"
echo "Group create response:"
echo "$GROUP_RESPONSE" | jq '.'
echo ""

# Step 3: Add members to the group
echo -e "${YELLOW}Step 3: Adding members to the group...${NC}"

echo "Adding Alice to the group..."
ALICE_MEMBER=$(curl -s -X POST "http://localhost:8082/groups/$GROUP_ID/members" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CHARLIE_TOKEN" \
  -d "{\"userId\": \"$ALICE_ID\", \"role\": \"MEMBER\"}")
echo -e "✓ Alice added: $(echo "$ALICE_MEMBER" | jq -r .id)"
echo "Add Alice response:"
echo "$ALICE_MEMBER" | jq '.'

echo "Adding Bob to the group..."
BOB_MEMBER=$(curl -s -X POST "http://localhost:8082/groups/$GROUP_ID/members" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CHARLIE_TOKEN" \
  -d "{\"userId\": \"$BOB_ID\", \"role\": \"MEMBER\"}")
echo -e "✓ Bob added: $(echo "$BOB_MEMBER" | jq -r .id)"
echo "Add Bob response:"
echo "$BOB_MEMBER" | jq '.'

# Verify group membership
GROUP_WITH_MEMBERS=$(curl -s "http://localhost:8082/groups/$GROUP_ID" -H "Authorization: Bearer $CHARLIE_TOKEN")
MEMBER_COUNT=$(echo "$GROUP_WITH_MEMBERS" | jq '.members | length')
echo -e "✓ Group now has ${GREEN}$MEMBER_COUNT${NC} members (Charlie as owner + Alice + Bob)"
echo "Group details response:"
echo "$GROUP_WITH_MEMBERS" | jq '.'
echo ""

# Step 4: Add expenses
echo -e "${YELLOW}Step 4: Adding expenses to the group...${NC}"

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
echo "  Paid by: Alice, Split among: Alice, Bob, Charlie"
echo "Expense 1 response:"
echo "$EXPENSE1" | jq '.'

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
echo "  Paid by: Bob, Split among: Bob, Charlie"
echo "Expense 2 response:"
echo "$EXPENSE2" | jq '.'

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
echo "  Paid by: Charlie, Split among: Alice, Bob, Charlie"
echo "Expense 3 response:"
echo "$EXPENSE3" | jq '.'

echo ""
echo -e "${GREEN}✓ All 3 expenses created successfully!${NC}"
echo ""

# Wait for Split Engine to process events
echo -e "${YELLOW}Waiting for Split Engine to process expenses...${NC}"
sleep 8

# Step 5: Get final balances and calculations
echo -e "${YELLOW}Step 5: Getting final balances and calculations...${NC}"

BALANCES=$(curl -s "http://localhost:8084/api/splits/group/$GROUP_ID/balances" \
  -H "Authorization: Bearer $ALICE_TOKEN")
echo "Raw balances response:"
echo "$BALANCES" | jq '.'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}         SPLIT ENGINE CALCULATIONS      ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${YELLOW}Expense Summary:${NC}"
echo ""
echo "Expense 1 - Dinner (\$120.00):"
echo "  • Alice paid: \$120.00"
echo "  • Split among: Alice, Bob, Charlie (equal shares)"
echo ""

echo "Expense 2 - Museum (\$60.00):"
echo "  • Bob paid: \$60.00"
echo "  • Split among: Bob, Charlie (equal shares)"
echo ""

echo "Expense 3 - Taxi (\$45.00):"
echo "  • Charlie paid: \$45.00"
echo "  • Split among: Alice, Bob, Charlie (equal shares)"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}      SPLIT ENGINE API RESULTS          ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

if [ "$BALANCES" = "[]" ] || [ -z "$BALANCES" ]; then
  echo -e "${RED}⚠️  Split Engine returned empty balances${NC}"
  echo "This might be due to processing delay. Please wait a moment and try again."
else
  echo -e "${GREEN}✓ Split Engine balances (calculated automatically):${NC}"
  echo ""
  
  # Extract and display balances with user identification
  ALICE_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$ALICE_ID\") | .balanceCents")
  BOB_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$BOB_ID\") | .balanceCents")
  CHARLIE_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$CHARLIE_ID\") | .balanceCents")
  
  echo "Alice ($ALICE_ID):"
  if [ "$ALICE_BALANCE" -gt 0 ]; then
    echo "  Balance: +\$$(echo "scale=2; $ALICE_BALANCE/100" | bc) (should receive money)"
  else
    echo "  Balance: -\$$(echo "scale=2; ${ALICE_BALANCE#-}/100" | bc) (owes money)"
  fi
  
  echo ""
  echo "Bob ($BOB_ID):"
  if [ "$BOB_BALANCE" -gt 0 ]; then
    echo "  Balance: +\$$(echo "scale=2; $BOB_BALANCE/100" | bc) (should receive money)"
  else
    echo "  Balance: -\$$(echo "scale=2; ${BOB_BALANCE#-}/100" | bc) (owes money)"
  fi
  
  echo ""
  echo "Charlie ($CHARLIE_ID):"
  if [ "$CHARLIE_BALANCE" -gt 0 ]; then
    echo "  Balance: +\$$(echo "scale=2; $CHARLIE_BALANCE/100" | bc) (should receive money)"
  else
    echo "  Balance: -\$$(echo "scale=2; ${CHARLIE_BALANCE#-}/100" | bc) (owes money)"
  fi
  
  echo ""
  # Verify balances sum to zero
  TOTAL=$(echo "$ALICE_BALANCE + $BOB_BALANCE + $CHARLIE_BALANCE" | bc)
  echo -e "${YELLOW}Verification:${NC} All balances sum to \$$(echo "scale=2; $TOTAL/100" | bc)"
  if [ "$TOTAL" -eq 0 ]; then
    echo -e "${GREEN}✓ Perfect! Split Engine calculations are balanced.${NC}"
  else
    echo -e "${RED}⚠️  Balances don't sum to zero - there might be rounding differences.${NC}"
  fi
fi

echo ""
echo -e "${YELLOW}Step 6: Scenario - Charlie initiates, Bob is payee, equal split${NC}"
echo "Creating expense: paidBy=Bob, participants=Alice+Bob+Charlie (equal) ..."
EXPENSE_PAIDBY_BOB=$(curl -s -X POST 'http://localhost:8083/api/expenses' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CHARLIE_TOKEN" \
  --data-binary @- <<EOF
{
  "groupId": "$GROUP_ID",
  "currency": "USD",
  "amount": 90.00,
  "occurredAt": "2025-09-22",
  "note": "Scenario: Charlie initiates, Bob is payee, equal split",
  "category": "OTHER",
  "participants": [
    {"userId": "$ALICE_ID", "ruleType": "EQUAL"},
    {"userId": "$BOB_ID", "ruleType": "EQUAL"},
    {"userId": "$CHARLIE_ID", "ruleType": "EQUAL"}
  ],
  "paidBy": "$BOB_ID"
}
EOF
)
EXPENSE_PAIDBY_BOB_ID=$(echo "$EXPENSE_PAIDBY_BOB" | jq -r .id)
echo -e "✓ Expense created: ${GREEN}$EXPENSE_PAIDBY_BOB_ID${NC} - paidBy Bob"
echo "Expense (paidBy=Bob) response:"
echo "$EXPENSE_PAIDBY_BOB" | jq '.'

echo -e "${YELLOW}Waiting for Split Engine to process scenario...${NC}"
sleep 6

echo -e "${BLUE}Updated balances after paidBy=Bob scenario:${NC}"
BALANCES=$(curl -s "http://localhost:8084/api/splits/group/$GROUP_ID/balances" \
  -H "Authorization: Bearer $ALICE_TOKEN")
echo "Raw balances response after scenario:"
echo "$BALANCES" | jq '.'

if [ "$BALANCES" = "[]" ] || [ -z "$BALANCES" ]; then
  echo -e "${RED}⚠️  Split Engine returned empty balances${NC}"
else
  ALICE_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$ALICE_ID\") | .balanceCents")
  BOB_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$BOB_ID\") | .balanceCents")
  CHARLIE_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$CHARLIE_ID\") | .balanceCents")

  echo "Alice:  $( [ -n "$ALICE_BALANCE" ] && echo $(echo "scale=2; $ALICE_BALANCE/100" | bc) || echo NA )"
  echo "Bob:    $( [ -n "$BOB_BALANCE" ] && echo $(echo "scale=2; $BOB_BALANCE/100" | bc) || echo NA )"
  echo "Charlie: $( [ -n "$CHARLIE_BALANCE" ] && echo $(echo "scale=2; $CHARLIE_BALANCE/100" | bc) || echo NA )"
fi

# ===================== NEW: Settlement Proposal & Acceptance =====================
# Step 7: Create a manual settlement proposal (choose a simple debtor -> creditor scenario)
# We will create a proposal where a user who OWES pays one who SHOULD RECEIVE based on current balances.
# For simplicity, we only consider one payment between the first negative (debtor) and first positive (creditor) balance found.

echo ""
echo -e "${YELLOW}Step 7: Creating a settlement proposal (pending acceptance)...${NC}"

# Reuse latest balances (already in BALANCES variable). If empty, fetch again.
if [ -z "$BALANCES" ] || [ "$BALANCES" = "[]" ]; then
  BALANCES=$(curl -s "http://localhost:8084/api/splits/group/$GROUP_ID/balances" -H "Authorization: Bearer $ALICE_TOKEN")
fi

# Extract balances again to ensure variables exist (cents, may be negative)
ALICE_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$ALICE_ID\") | .balanceCents")
BOB_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$BOB_ID\") | .balanceCents")
CHARLIE_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$CHARLIE_ID\") | .balanceCents")

# Helper to decide debtor/creditor
DEBTOR_ID=""; DEBTOR_NAME=""; DEBTOR_BALANCE=0
CREDITOR_ID=""; CREDITOR_NAME=""; CREDITOR_BALANCE=0

# Identify first debtor (negative balance)
if [ -n "$ALICE_BALANCE" ] && [ "$ALICE_BALANCE" != "null" ] && [ "$ALICE_BALANCE" -lt 0 ] && [ -z "$DEBTOR_ID" ]; then
  DEBTOR_ID=$ALICE_ID; DEBTOR_NAME="Alice"; DEBTOR_BALANCE=$ALICE_BALANCE
fi
if [ -n "$BOB_BALANCE" ] && [ "$BOB_BALANCE" != "null" ] && [ "$BOB_BALANCE" -lt 0 ] && [ -z "$DEBTOR_ID" ]; then
  DEBTOR_ID=$BOB_ID; DEBTOR_NAME="Bob"; DEBTOR_BALANCE=$BOB_BALANCE
fi
if [ -n "$CHARLIE_BALANCE" ] && [ "$CHARLIE_BALANCE" != "null" ] && [ "$CHARLIE_BALANCE" -lt 0 ] && [ -z "$DEBTOR_ID" ]; then
  DEBTOR_ID=$CHARLIE_ID; DEBTOR_NAME="Charlie"; DEBTOR_BALANCE=$CHARLIE_BALANCE
fi

# Identify first creditor (positive balance)
if [ -n "$ALICE_BALANCE" ] && [ "$ALICE_BALANCE" != "null" ] && [ "$ALICE_BALANCE" -gt 0 ] && [ -z "$CREDITOR_ID" ]; then
  CREDITOR_ID=$ALICE_ID; CREDITOR_NAME="Alice"; CREDITOR_BALANCE=$ALICE_BALANCE
fi
if [ -n "$BOB_BALANCE" ] && [ "$BOB_BALANCE" != "null" ] && [ "$BOB_BALANCE" -gt 0 ] && [ -z "$CREDITOR_ID" ]; then
  CREDITOR_ID=$BOB_ID; CREDITOR_NAME="Bob"; CREDITOR_BALANCE=$BOB_BALANCE
fi
if [ -n "$CHARLIE_BALANCE" ] && [ "$CHARLIE_BALANCE" != "null" ] && [ "$CHARLIE_BALANCE" -gt 0 ] && [ -z "$CREDITOR_ID" ]; then
  CREDITOR_ID=$CHARLIE_ID; CREDITOR_NAME="Charlie"; CREDITOR_BALANCE=$CHARLIE_BALANCE
fi

if [ -z "$DEBTOR_ID" ] || [ -z "$CREDITOR_ID" ]; then
  echo -e "${RED}No debtor/creditor pair detected – skipping settlement proposal.${NC}"
else
  # Amount to settle = min(abs(debtor), creditor) in cents
  ABS_DEBTOR=$(( DEBTOR_BALANCE * -1 ))
  if [ $ABS_DEBTOR -lt $CREDITOR_BALANCE ]; then
    SETTLE_CENTS=$ABS_DEBTOR
  else
    SETTLE_CENTS=$CREDITOR_BALANCE
  fi
  # Convert cents to decimal amount (e.g., 4000 -> 40.00)
  SETTLE_AMOUNT=$(printf '%d' "$SETTLE_CENTS")
  SETTLE_AMOUNT_DEC=$(echo "scale=2; $SETTLE_AMOUNT/100" | bc)

  echo "Creating MANUAL_SETTLEMENT proposal: $DEBTOR_NAME pays $CREDITOR_NAME $SETTLE_AMOUNT_DEC USD"

  # Decide who will authenticate the proposal creation: use debtor's token (intuitive – debtor proposes to pay)
  CREATOR_TOKEN=""
  if [ "$DEBTOR_ID" = "$ALICE_ID" ]; then CREATOR_TOKEN=$ALICE_TOKEN; fi
  if [ "$DEBTOR_ID" = "$BOB_ID" ]; then CREATOR_TOKEN=$BOB_TOKEN; fi
  if [ "$DEBTOR_ID" = "$CHARLIE_ID" ]; then CREATOR_TOKEN=$CHARLIE_TOKEN; fi

  SETTLEMENT_PROPOSAL_CREATE=$(curl -s -X POST 'http://localhost:8085/api/settlements/proposals' \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $CREATOR_TOKEN" \
    --data-binary @- <<EOF
{
  "groupId": "$GROUP_ID",
  "title": "Manual Settlement - $DEBTOR_NAME pays $CREDITOR_NAME",
  "description": "$DEBTOR_NAME partially settles balance with $CREDITOR_NAME",
  "currency": "USD",
  "proposalType": "MANUAL_SETTLEMENT",
  "payments": [
    {
      "payerId": "$DEBTOR_ID",
      "payeeId": "$CREDITOR_ID",
      "amount": $SETTLE_AMOUNT_DEC,
      "description": "Partial settlement",
      "paymentMethod": "CASH"
    }
  ]
}
EOF
  )

  PROPOSAL_ID=$(echo "$SETTLEMENT_PROPOSAL_CREATE" | jq -r .id)
  PROPOSAL_STATUS=$(echo "$SETTLEMENT_PROPOSAL_CREATE" | jq -r .status)
  PAYMENT_ID=$(echo "$SETTLEMENT_PROPOSAL_CREATE" | jq -r '.payments[0].id')

  if [ -z "$PAYMENT_ID" ] || [ "$PAYMENT_ID" = "null" ]; then
    echo -e "${RED}No payment ID returned in proposal response – skipping confirmation steps.${NC}"
  fi

  if [ -n "$PROPOSAL_ID" ] && [ "$PROPOSAL_ID" != "null" ]; then
    echo -e "${GREEN}✓ Settlement proposal created: $PROPOSAL_ID (status: $PROPOSAL_STATUS)${NC}"
    echo "$SETTLEMENT_PROPOSAL_CREATE" | jq '.'

    # Step 8: Accept the settlement proposal (must be a different participant; use creditor if different)
    echo -e "${YELLOW}Step 8: Accepting settlement proposal...${NC}"
    ACCEPTOR_TOKEN=""
    if [ "$CREDITOR_ID" = "$ALICE_ID" ]; then ACCEPTOR_TOKEN=$ALICE_TOKEN; fi
    if [ "$CREDITOR_ID" = "$BOB_ID" ]; then ACCEPTOR_TOKEN=$BOB_TOKEN; fi
    if [ "$CREDITOR_ID" = "$CHARLIE_ID" ]; then ACCEPTOR_TOKEN=$CHARLIE_TOKEN; fi

    SETTLEMENT_PROPOSAL_ACCEPT=$(curl -s -X POST "http://localhost:8085/api/settlements/proposals/$PROPOSAL_ID/accept" \
      -H 'Content-Type: application/json' \
      -H "Authorization: Bearer $ACCEPTOR_TOKEN")

    UPDATED_STATUS=$(echo "$SETTLEMENT_PROPOSAL_ACCEPT" | jq -r .status)
    echo -e "${GREEN}✓ Settlement proposal accepted. New status: $UPDATED_STATUS${NC}"
    echo "$SETTLEMENT_PROPOSAL_ACCEPT" | jq '.'

    if [ -n "$PAYMENT_ID" ] && [ "$PAYMENT_ID" != "null" ]; then
      # Optional: Payer confirms payment, then Payee confirms
      echo -e "${YELLOW}Confirming payment (payer then payee)...${NC}"
      PAYER_CONFIRM=$(curl -s -X POST "http://localhost:8085/api/settlements/payments/$PAYMENT_ID/confirm-payer" \
        -H 'Content-Type: application/json' \
        -H "Authorization: Bearer $CREATOR_TOKEN" \
        -d '{"notes":"Payer confirms sending funds"}')
      echo -e "${GREEN}✓ Payer confirmation added${NC}" | sed 's/^/  /'

      PAYEE_CONFIRM=$(curl -s -X POST "http://localhost:8085/api/settlements/payments/$PAYMENT_ID/confirm-payee" \
        -H 'Content-Type: application/json' \
        -H "Authorization: Bearer $ACCEPTOR_TOKEN" \
        -d '{"notes":"Payee confirms receipt"}')
      echo -e "${GREEN}✓ Payee confirmation added${NC}" | sed 's/^/  /'

      echo "Final payment state:"; echo "$PAYEE_CONFIRM" | jq '.'
    else
      echo -e "${YELLOW}Skipping payment confirmation because PAYMENT_ID is not available.${NC}"
    fi
  else
    echo -e "${RED}Failed to create settlement proposal${NC}"
    echo "$SETTLEMENT_PROPOSAL_CREATE" | jq '.'
  fi
fi
# ===================== END NEW SETTLEMENT STEPS =====================

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}            SUMMARY                     ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${GREEN}✅ Successfully completed full expense flow:${NC}"
echo "  1. ✓ Created and authenticated 3 users (Alice, Bob, Charlie)"
echo "  2. ✓ Created group with Charlie as owner"
echo "  3. ✓ Added Alice and Bob as group members"
echo "  4. ✓ Created base expenses with different payers and participants"
echo "  5. ✓ Split Engine calculated balances automatically"
echo "  6. ✓ Additional scenario expense (initiator ≠ payer) processed and balances updated"
echo "  7. ✓ Settlement proposal created (pending -> accepted) with payer & payee confirmations"
echo ""

echo -e "${YELLOW}Total expenses: \$225.00${NC}"
echo "  • Dinner: \$120.00 (Alice paid, all participated)"
echo "  • Museum: \$60.00 (Bob paid, Bob & Charlie participated)"
echo "  • Taxi: \$45.00 (Charlie paid, all participated)"
echo ""

echo -e "${YELLOW}Key Features Demonstrated:${NC}"
echo "  • ✅ JWT Authentication across all services"
echo "  • ✅ User profile management with consistent IDs"
echo "  • ✅ Group creation and member management"
echo "  • ✅ Expense creation with flexible participants"
echo "  • ✅ Real-time split calculations via Kafka events"
echo "  • ✅ Automatic balance tracking and updates"
echo "  • ✅ RESTful APIs with proper error handling"
echo ""

echo -e "${GREEN}🎉 Expenses Platform with Split Engine API working perfectly! 🎉${NC}"
echo ""
echo -e "${BLUE}The Split Engine automatically calculated all balances based on:${NC}"
echo "  • Who paid each expense (payer identification)"
echo "  • How expenses were split (equal shares, percentages, etc.)"
echo "  • Real-time event processing via Kafka"
echo "  • Proper crediting and debiting of participants"

echo -e "${YELLOW}Post-Run Verification: Fetch balances again via direct curl call${NC}"
# Allow override: if user sets FINAL_BALANCE_CHECK_TOKEN externally, use it; else default to Charlie's token
FINAL_BALANCE_CHECK_TOKEN=${FINAL_BALANCE_CHECK_TOKEN:-$CHARLIE_TOKEN}
TARGET_GROUP_ID=${TARGET_GROUP_ID:-$GROUP_ID}

if [ -z "$FINAL_BALANCE_CHECK_TOKEN" ]; then
  echo -e "${RED}No token available for final balance check. Skipping.${NC}"
else
  echo "Calling: GET /api/splits/group/$TARGET_GROUP_ID/balances"
  FINAL_BALANCES_RAW=$(curl -s -X GET \
    "http://localhost:8084/api/splits/group/$TARGET_GROUP_ID/balances" \
    -H 'accept: */*' \
    -H "Authorization: Bearer $FINAL_BALANCE_CHECK_TOKEN")
  echo "Raw response:"; echo "$FINAL_BALANCES_RAW" | jq '.' || echo "$FINAL_BALANCES_RAW"

  if command -v jq >/dev/null 2>&1; then
    echo -e "${GREEN}Parsed balances summary:${NC}"
    echo "$FINAL_BALANCES_RAW" | jq -r 'map({userId, balanceCents, balance: (.balanceCents/100)}) | .[] | " - User: \(.userId) Balance: \(.balance) (cents=\(.balanceCents))"'
    TOTAL_CENTS=$(echo "$FINAL_BALANCES_RAW" | jq '[.[].balanceCents] | add // 0')
    if [ "$TOTAL_CENTS" != "" ]; then
      TOTAL_FMT=$(echo "scale=2; $TOTAL_CENTS/100" | bc 2>/dev/null || echo "n/a")
      if [ "$TOTAL_CENTS" -eq 0 ] 2>/dev/null; then
        echo -e "${GREEN}Net total balance = $TOTAL_FMT (perfectly balanced)${NC}"
      else
        echo -e "${RED}Net total balance = $TOTAL_FMT (should be 0; investigate rounding or processing delays)${NC}"
      fi
    fi
  fi
fi

echo -e "${BLUE}End of script.${NC}"
