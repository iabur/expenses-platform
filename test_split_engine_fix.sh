#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  TESTING SPLIT ENGINE FIX             ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Get tokens
echo -e "${YELLOW}Getting JWT tokens...${NC}"
ALICE_TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/expenses/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=web&username=alice@example.com&password=password' | jq -r .access_token)

BOB_TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/expenses/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=web&username=bob@example.com&password=password' | jq -r .access_token)

CHARLIE_TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/expenses/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=web&username=charlie@example.com&password=password' | jq -r .access_token)

# Get user IDs
ALICE_ID="6af0ec47-62ab-482f-add4-0d446a54fd38"
BOB_ID="f2973bae-038b-4439-a167-8afef1ff3015"
CHARLIE_ID="66059af8-6f35-4154-93cc-f51c38d93e92"

echo -e "${GREEN}✓ Tokens obtained${NC}"
echo ""

# Create a new test group
echo -e "${YELLOW}Creating new test group...${NC}"
TEST_GROUP=$(curl -s -X POST 'http://localhost:8082/groups' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CHARLIE_TOKEN" \
  -d '{
    "name": "Split Engine Test",
    "description": "Testing correct payer identification",
    "defaultCurrency": "USD"
  }')

TEST_GROUP_ID=$(echo "$TEST_GROUP" | jq -r .id)
echo -e "✓ Test group created: ${GREEN}$TEST_GROUP_ID${NC}"

# Add members
echo "Adding Alice and Bob to the group..."
curl -s -X POST "http://localhost:8082/groups/$TEST_GROUP_ID/members" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CHARLIE_TOKEN" \
  -d "{\"userId\": \"$ALICE_ID\", \"role\": \"MEMBER\"}" > /dev/null

curl -s -X POST "http://localhost:8082/groups/$TEST_GROUP_ID/members" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CHARLIE_TOKEN" \
  -d "{\"userId\": \"$BOB_ID\", \"role\": \"MEMBER\"}" > /dev/null

echo -e "${GREEN}✓ Members added${NC}"
echo ""

# Test Case 1: Alice pays $100, split 3 ways
echo -e "${YELLOW}Test 1: Alice pays \$100 dinner (split 3 ways)${NC}"
EXPENSE1=$(curl -s -X POST 'http://localhost:8083/api/expenses' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -d "{
    \"groupId\": \"$TEST_GROUP_ID\",
    \"currency\": \"USD\",
    \"amount\": 100.00,
    \"occurredAt\": \"2025-09-22\",
    \"note\": \"Dinner (Alice pays)\",
    \"category\": \"FOOD\",
    \"participants\": [
      {\"userId\": \"$ALICE_ID\", \"ruleType\": \"EQUAL\"},
      {\"userId\": \"$BOB_ID\", \"ruleType\": \"EQUAL\"},
      {\"userId\": \"$CHARLIE_ID\", \"ruleType\": \"EQUAL\"}
    ]
  }")
echo "✓ Expense 1 created: $(echo "$EXPENSE1" | jq -r .id)"

# Test Case 2: Bob pays $60, split 2 ways (Bob and Charlie)
echo -e "${YELLOW}Test 2: Bob pays \$60 tickets (split between Bob and Charlie)${NC}"
EXPENSE2=$(curl -s -X POST 'http://localhost:8083/api/expenses' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $BOB_TOKEN" \
  -d "{
    \"groupId\": \"$TEST_GROUP_ID\",
    \"currency\": \"USD\",
    \"amount\": 60.00,
    \"occurredAt\": \"2025-09-22\",
    \"note\": \"Tickets (Bob pays)\",
    \"category\": \"ENTERTAINMENT\",
    \"participants\": [
      {\"userId\": \"$BOB_ID\", \"ruleType\": \"EQUAL\"},
      {\"userId\": \"$CHARLIE_ID\", \"ruleType\": \"EQUAL\"}
    ]
  }")
echo "✓ Expense 2 created: $(echo "$EXPENSE2" | jq -r .id)"

# Test Case 3: Charlie pays $30, split 3 ways
echo -e "${YELLOW}Test 3: Charlie pays \$30 taxi (split 3 ways)${NC}"
EXPENSE3=$(curl -s -X POST 'http://localhost:8083/api/expenses' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CHARLIE_TOKEN" \
  -d "{
    \"groupId\": \"$TEST_GROUP_ID\",
    \"currency\": \"USD\",
    \"amount\": 30.00,
    \"occurredAt\": \"2025-09-22\",
    \"note\": \"Taxi (Charlie pays)\",
    \"category\": \"TRANSPORT\",
    \"participants\": [
      {\"userId\": \"$ALICE_ID\", \"ruleType\": \"EQUAL\"},
      {\"userId\": \"$BOB_ID\", \"ruleType\": \"EQUAL\"},
      {\"userId\": \"$CHARLIE_ID\", \"ruleType\": \"EQUAL\"}
    ]
  }")
echo "✓ Expense 3 created: $(echo "$EXPENSE3" | jq -r .id)"
echo ""

# Wait for Split Engine processing
echo -e "${YELLOW}Waiting for Split Engine to process expenses...${NC}"
sleep 8

# Get balances from Split Engine API
echo -e "${YELLOW}Getting balances from Split Engine API...${NC}"
BALANCES=$(curl -s "http://localhost:8084/api/splits/group/$TEST_GROUP_ID/balances" \
  -H "Authorization: Bearer $ALICE_TOKEN")

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}         EXPECTED CALCULATIONS          ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo "Test Expenses:"
echo "1. Alice pays \$100, split 3 ways = \$33.33 each"
echo "2. Bob pays \$60, split 2 ways = \$30 each"
echo "3. Charlie pays \$30, split 3 ways = \$10 each"
echo ""

echo "Expected Balances:"
echo "Alice:"
echo "  • Paid: \$100"
echo "  • Owes: \$33.33 (dinner) + \$10 (taxi) = \$43.33"
echo "  • Net: \$100 - \$43.33 = +\$56.67 (should receive)"
echo ""

echo "Bob:"
echo "  • Paid: \$60"
echo "  • Owes: \$33.33 (dinner) + \$30 (tickets) + \$10 (taxi) = \$73.33"
echo "  • Net: \$60 - \$73.33 = -\$13.33 (owes money)"
echo ""

echo "Charlie:"
echo "  • Paid: \$30"
echo "  • Owes: \$33.33 (dinner) + \$30 (tickets) + \$10 (taxi) = \$73.33"
echo "  • Net: \$30 - \$73.33 = -\$43.33 (owes money)"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}      SPLIT ENGINE API RESULTS          ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

if [ "$BALANCES" = "[]" ] || [ -z "$BALANCES" ]; then
  echo -e "${RED}⚠️  Split Engine returned empty balances${NC}"
  echo "Check Split Engine logs for processing issues."
else
  echo -e "${GREEN}✓ Split Engine balances:${NC}"
  echo "$BALANCES" | jq -r '.[] | 
    "User: " + .userId + 
    "\nBalance: $" + (.balanceCents/100 | tostring) + 
    " (" + (if .balanceCents > 0 then "should receive" else "owes money" end) + ")" + 
    "\nLast updated: " + .lastUpdated + "\n"'
  
  # Check if results match expectations
  echo -e "${YELLOW}Verification:${NC}"
  ALICE_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$ALICE_ID\") | .balanceCents")
  BOB_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$BOB_ID\") | .balanceCents")
  CHARLIE_BALANCE=$(echo "$BALANCES" | jq -r ".[] | select(.userId == \"$CHARLIE_ID\") | .balanceCents")
  
  echo "Alice balance: $(echo "scale=2; $ALICE_BALANCE/100" | bc) (expected: +56.67)"
  echo "Bob balance: $(echo "scale=2; $BOB_BALANCE/100" | bc) (expected: -13.33)"
  echo "Charlie balance: $(echo "scale=2; $CHARLIE_BALANCE/100" | bc) (expected: -43.33)"
  
  # Check if balances sum to zero
  TOTAL=$(echo "$ALICE_BALANCE + $BOB_BALANCE + $CHARLIE_BALANCE" | bc)
  echo "Total balance: $(echo "scale=2; $TOTAL/100" | bc) (should be 0.00)"
  
  if [ "$TOTAL" -eq 0 ]; then
    echo -e "${GREEN}✓ Balances are correctly balanced!${NC}"
  else
    echo -e "${RED}✗ Balances don't sum to zero - there might be an issue${NC}"
  fi
fi

echo ""
echo -e "${GREEN}🎉 Split Engine testing completed! 🎉${NC}"



