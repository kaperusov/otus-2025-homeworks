#!/bin/bash

# Базовый URL приложения
BASE_URL="http://arch.homework"

# Функция для генерации случайного имени пользователя
generate_random_name() {
    local vowels="aeiou"
    local consonants="bcdfghjklmnpqrstvwxyz"
    local name=""
    for i in {1..6}; do
        if (( RANDOM % 2 )); then
            name+=${consonants:$((RANDOM % ${#consonants})):1}
        else
            name+=${vowels:$((RANDOM % ${#vowels})):1}
        fi
    done
    echo "$name"
}

# Функция для генерации случайного email
generate_random_email() {
    local name=$(generate_random_name)
    echo "${name}@example.com"
}

# Функция для проверки здоровья сервиса
check_health() {
    echo "[$(date)] Checking health..."
    curl -s -X GET "$BASE_URL/health" 
    echo ""
}

# Функция для проверки готовности сервиса
check_ready() {
    echo "[$(date)] Checking readiness..."
    curl -s -X GET "$BASE_URL/ready" 
    echo ""
}

# Функция для создания пользователя
create_user() {
    local first_name=$(generate_random_name)
    local last_name=$(generate_random_name)
    local email=$(generate_random_email)
    local phone="7$((RANDOM % 9000000 + 1000000))"
    local username="${first_name:0:1}${last_name:0:1}"
    
    echo "[$(date)] Creating user: $first_name $last_name ($email)"
    
    local response=$(curl -s -X POST "$BASE_URL/api/v1/users" \
        -H "Content-Type: application/json" \
        -d "{
            \"firstName\": \"$first_name\",
            \"lastName\": \"$last_name\",
            \"email\": \"$email\",
            \"phone\": \"$phone\",
            \"username\": \"$username\"
        }")
    
    echo "$response"
    # Извлекаем ID созданного пользователя
    local user_id=$(echo "$response" | jq -r '.id')
    echo "$user_id" >> user_ids.txt
    echo "Created user ID: $user_id"
}

# Функция для получения пользователя
get_user() {
    if [ ! -f "user_ids.txt" ] || [ ! -s "user_ids.txt" ]; then
        echo "No users to get"
        return
    fi
    
    # Выбираем случайный ID из файла
    local user_id=$(shuf -n 1 user_ids.txt)
    if [ -z "$user_id" ]; then
        echo "No valid user ID found"
        return
    fi
    
    echo "[$(date)] Getting user with ID: $user_id"
    curl -s -X GET "$BASE_URL/api/v1/users/$user_id"
    echo ""
}

# Функция для обновления пользователя
update_user() {
    if [ ! -f "user_ids.txt" ] || [ ! -s "user_ids.txt" ]; then
        echo "No users to update"
        return
    fi
    
    # Выбираем случайный ID из файла
    local user_id=$(shuf -n 1 user_ids.txt)
    if [ -z "$user_id" ]; then
        echo "No valid user ID found"
        return
    fi
    
    local new_email=$(generate_random_email)
    
    echo "[$(date)] Updating user with ID: $user_id, new email: $new_email"
    curl -s -X PUT "$BASE_URL/api/v1/users/$user_id" \
        -H "Content-Type: application/json" \
        -d "{\"email\": \"$new_email\"}"
    echo ""
}

# Функция для удаления пользователя
delete_user() {
    if [ ! -f "user_ids.txt" ] || [ ! -s "user_ids.txt" ]; then
        echo "No users to delete"
        return
    fi
    
    # Выбираем случайный ID из файла
    local user_id=$(shuf -n 1 user_ids.txt)
    if [ -z "$user_id" ]; then
        echo "No valid user ID found"
        return
    fi
    
    echo "[$(date)] Deleting user with ID: $user_id"
    curl -s -X DELETE "$BASE_URL/api/v1/users/$user_id"
    echo ""
    
    # Удаляем ID из файла
    sed -i "/^$user_id$/d" user_ids.txt
}

# Основной цикл генерации трафика
while true; do
    # Случайно выбираем действие
    case $((RANDOM % 10)) in
        0|1)
            check_health
            ;;
        2)
            check_ready
            ;;
        3|4|5)
            create_user
            ;;
        6|7)
            get_user
            ;;
        8)
            update_user
            ;;
        9)
            delete_user
            ;;
    esac
    
    # Случайная пауза между запросами (0.5 - 3 секунды)
    sleep $(awk "BEGIN {print 0.5 + ($RANDOM / 32767 * 2.5)}")
done