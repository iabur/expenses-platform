#!/bin/bash

echo "=== 💰 SETTLEMENT BALANCE CHECKER ==="
echo ""

# Step 1: Get JWT Token
echo "🔐 Getting JWT Token..."
JWT_TOKEN=$(curl -s -X POST "http://localhost:8090/realms/expenses/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser" \
  -d "password=password123" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ "$JWT_TOKEN" = "null" ] || [ -z "$JWT_TOKEN" ]; then
    echo "❌ Failed to get JWT token"
    exit 1
fi

echo "✅ JWT Token obtained"
echo ""

# Step 2: Get User Profile
echo "👤 Getting your profile..."
USER_PROFILE=$(curl -s -X GET "http://localhost:8081/user/me" \
  -H "Authorization: Bearer $JWT_TOKEN")

echo "$USER_PROFILE" | jq '{id, firstName, lastName, email}'
echo ""

# Step 3: Check Settlement Balance
echo "💰 Checking your settlement balance..."
BALANCE=$(curl -s -X GET "http://localhost:8085/api/settlements/my/balance" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json")

echo "$BALANCE" | jq '.'
echo ""

# Step 4: Check Group Balance
echo "👥 Checking group balance..."
GROUP_BALANCE=$(curl -s -X GET "http://localhost:8085/api/settlements/group/156ac659-a741-401b-9c21-6b0a5787c13e/balance" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json")

echo "$GROUP_BALANCE" | jq '.'
echo ""

# Step 5: Get Your Expenses
echo "📋 Your expenses (what you paid)..."
MY_EXPENSES=$(curl -s -X GET "http://localhost:8083/api/expenses/my" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json")

echo "$MY_EXPENSES" | jq '.content[] | {id, title, amount, currency, createdAt}'
echo ""

# Step 6: Get Participated Expenses
echo "📝 Expenses you participated in (what you owe)..."
PARTICIPATED=$(curl -s -X GET "http://localhost:8083/api/expenses/participated" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json")

echo "$PARTICIPATED" | jq '.content[] | {id, title, amount, currency, createdAt}'
echo ""

# Step 7: Get Simplified Debts
echo "🧮 Simplified debts for the group..."
SIMPLIFIED=$(curl -s -X GET "http://localhost:8084/api/splits/group/156ac659-a741-401b-9c21-6b0a5787c13e/simplify" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json")

echo "$SIMPLIFIED" | jq '.'
echo ""

echo "=== 🎯 SUMMARY ==="
echo "Use these commands to check your balance anytime:"
echo ""
echo "1. Get your balance:"
echo "curl -X GET \"http://localhost:8085/api/settlements/my/balance\" -H \"Authorization: Bearer \$JWT_TOKEN\""
echo ""
echo "2. Get group balance:"
echo "curl -X GET \"http://localhost:8085/api/settlements/group/156ac659-a741-401b-9c21-6b0a5787c13e/balance\" -H \"Authorization: Bearer \$JWT_TOKEN\""
echo ""
echo "3. Get simplified debts:"
echo "curl -X GET \"http://localhost:8084/api/splits/group/156ac659-a741-401b-9c21-6b0a5787c13e/simplify\" -H \"Authorization: Bearer \$JWT_TOKEN\""

