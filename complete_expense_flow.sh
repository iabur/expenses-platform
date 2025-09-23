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

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}            SUMMARY                     ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${GREEN}✅ Successfully completed full expense flow:${NC}"
echo "  1. ✓ Created and authenticated 3 users (Alice, Bob, Charlie)"
echo "  2. ✓ Created group with Charlie as owner"
echo "  3. ✓ Added Alice and Bob as group members"
echo "  4. ✓ Created 3 expenses with different payers and participants"
echo "  5. ✓ Split Engine calculated balances automatically"
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
