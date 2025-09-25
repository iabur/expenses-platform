#!/bin/bash
set -e

echo "=== Step 0: Getting tokens for all users ==="
ALICE_TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/expenses/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=web&username=alice@example.com&password=password' | jq -r .access_token)

BOB_TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/expenses/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=web&username=bob@example.com&password=password' | jq -r .access_token)

CHARLIE_TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/expenses/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=web&username=charlie@example.com&password=password' | jq -r .access_token)

echo "✓ Tokens obtained for all users"

echo ""
echo "=== Step 1: Provision user profiles and get keycloakUserIds ==="

ALICE_PROFILE=$(curl -s 'http://localhost:8081/user/me' -H "Authorization: Bearer $ALICE_TOKEN")
ALICE_ID=$(echo "$ALICE_PROFILE" | jq -r .keycloakUserId)
echo "Alice: $ALICE_ID ($(echo "$ALICE_PROFILE" | jq -r .email))"

BOB_PROFILE=$(curl -s 'http://localhost:8081/user/me' -H "Authorization: Bearer $BOB_TOKEN")
BOB_ID=$(echo "$BOB_PROFILE" | jq -r .keycloakUserId)
echo "Bob: $BOB_ID ($(echo "$BOB_PROFILE" | jq -r .email))"

CHARLIE_PROFILE=$(curl -s 'http://localhost:8081/user/me' -H "Authorization: Bearer $CHARLIE_TOKEN")
CHARLIE_ID=$(echo "$CHARLIE_PROFILE" | jq -r .keycloakUserId)
echo "Charlie: $CHARLIE_ID ($(echo "$CHARLIE_PROFILE" | jq -r .email))"

echo ""
echo "=== Step 2: Create a group (Charlie's token) ==="

GROUP_RESPONSE=$(curl -s -X POST 'http://localhost:8082/groups' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CHARLIE_TOKEN" \
  -d '{
    "name": "Weekend Trip",
    "description": "Paris weekend getaway",
    "defaultCurrency": "USD"
  }')

GROUP_ID=$(echo "$GROUP_RESPONSE" | jq -r .id)
echo "✓ Group created: $GROUP_ID"
echo "Group details:" 
echo "$GROUP_RESPONSE" | jq '{id, name, createdBy, memberCount}'

echo ""
echo "=== Step 3: Add members to the group ==="

# Add Alice
ALICE_MEMBER=$(curl -s -X POST "http://localhost:8082/groups/$GROUP_ID/members" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CHARLIE_TOKEN" \
  -d "{\"userId\": \"$ALICE_ID\", \"role\": \"MEMBER\"}")
echo "✓ Added Alice: $(echo "$ALICE_MEMBER" | jq -r .id)"

# Add Bob
BOB_MEMBER=$(curl -s -X POST "http://localhost:8082/groups/$GROUP_ID/members" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CHARLIE_TOKEN" \
  -d "{\"userId\": \"$BOB_ID\", \"role\": \"MEMBER\"}")
echo "✓ Added Bob: $(echo "$BOB_MEMBER" | jq -r .id)"

# Verify group membership
GROUP_WITH_MEMBERS=$(curl -s "http://localhost:8082/groups/$GROUP_ID" -H "Authorization: Bearer $CHARLIE_TOKEN")
echo "Group now has $(echo "$GROUP_WITH_MEMBERS" | jq '.members | length') members:"
echo "$GROUP_WITH_MEMBERS" | jq '.members[] | {userId, role}'

echo ""
echo "=== Step 4: Add three expenses ==="

# Expense 1: Dinner (Alice, Bob, Charlie) - $100 split 3 ways = $33.33 each
EXPENSE1=$(curl -s -X POST 'http://localhost:8083/api/expenses' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -d "{
    \"groupId\": \"$GROUP_ID\",
    \"currency\": \"USD\",
    \"amount\": 100.00,
    \"occurredAt\": \"2025-09-22\",
    \"note\": \"Dinner\",
    \"category\": \"FOOD\",
    \"participants\": [
      {\"userId\": \"$ALICE_ID\", \"ruleType\": \"EQUAL\"},
      {\"userId\": \"$BOB_ID\", \"ruleType\": \"EQUAL\"},
      {\"userId\": \"$CHARLIE_ID\", \"ruleType\": \"EQUAL\"}
    ]
  }")
echo "✓ Expense 1 (Dinner): $(echo "$EXPENSE1" | jq -r .id)"

# Expense 2: Taxi (Alice, Bob) - $45.50 split 2 ways = $22.75 each
EXPENSE2=$(curl -s -X POST 'http://localhost:8083/api/expenses' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -d "{
    \"groupId\": \"$GROUP_ID\",
    \"currency\": \"USD\",
    \"amount\": 45.50,
    \"occurredAt\": \"2025-09-22\",
    \"note\": \"Taxi\",
    \"category\": \"TRANSPORT\",
    \"participants\": [
      {\"userId\": \"$ALICE_ID\", \"ruleType\": \"EQUAL\"},
      {\"userId\": \"$BOB_ID\", \"ruleType\": \"EQUAL\"}
    ]
  }")
echo "✓ Expense 2 (Taxi): $(echo "$EXPENSE2" | jq -r .id)"

# Expense 3: Museum (Bob, Charlie) - $30 split 2 ways = $15 each
EXPENSE3=$(curl -s -X POST 'http://localhost:8083/api/expenses' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $BOB_TOKEN" \
  -d "{
    \"groupId\": \"$GROUP_ID\",
    \"currency\": \"USD\",
    \"amount\": 30.00,
    \"occurredAt\": \"2025-09-22\",
    \"note\": \"Museum\",
    \"category\": \"ENTERTAINMENT\",
    \"participants\": [
      {\"userId\": \"$BOB_ID\", \"ruleType\": \"EQUAL\"},
      {\"userId\": \"$CHARLIE_ID\", \"ruleType\": \"EQUAL\"}
    ]
  }")
echo "✓ Expense 3 (Museum): $(echo "$EXPENSE3" | jq -r .id)"

echo ""
echo "=== Step 5: Final balances (who owes/gets paid) ==="

BALANCES=$(curl -s "http://localhost:8084/api/splits/group/$GROUP_ID/balances" \
  -H "Authorization: Bearer $ALICE_TOKEN")

echo "Final balances:"
echo "$BALANCES" | jq '.'

echo ""
echo "=== Summary ==="
echo "Expected calculation:"
echo "Alice paid: \$100 (dinner) + \$45.50 (taxi) = \$145.50"
echo "Alice owes: \$33.33 (dinner) + \$22.75 (taxi) = \$56.08"
echo "Alice balance: \$145.50 - \$56.08 = +\$89.42 (Alice should receive)"
echo ""
echo "Bob paid: \$30 (museum) = \$30"
echo "Bob owes: \$33.33 (dinner) + \$22.75 (taxi) + \$15 (museum) = \$71.08"
echo "Bob balance: \$30 - \$71.08 = -\$41.08 (Bob owes)"
echo ""
echo "Charlie paid: \$0"
echo "Charlie owes: \$33.33 (dinner) + \$15 (museum) = \$48.33"
echo "Charlie balance: \$0 - \$48.33 = -\$48.33 (Charlie owes)"
echo ""
echo "Verification: +\$89.42 - \$41.08 - \$48.33 = \$0.01 (rounding)"

