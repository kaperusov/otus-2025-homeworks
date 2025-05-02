#!/bin/bash

prompt() {
  local prompt_message="$1"
  local var_name="$2"
  local is_password="$3"
  local input=""
  
  while [ -z "$input" ]; do
    if [ "$is_password" = "true" ]; then
      read -s -p "$prompt_message: " input
      echo  # new line
    else
      read -p "$prompt_message: " input
    fi
    
    if [ -z "$input" ]; then
      echo "Ошибка: значение не может быть пустым. Пожалуйста, попробуйте еще раз."
    fi
  done
  
  eval "$var_name=\"$input\""
}

# Функция для ввода пароля с проверкой
get_password() {
  local password1
  local password2
  
  while true; do
    prompt "Введите пароль (ввод скрыт)" password1 true
    prompt "Повторите пароль (ввод скрыт)" password2 true
    
    if [ "$password1" != "$password2" ]; then
      echo "Ошибка: пароли не совпадают. Пожалуйста, попробуйте еще раз."
    else
      eval "$1=\"$password1\""
      break
    fi
  done
}

# --------------------------------------------------
# Запрашиваем данные у пользователя
echo "Конфигурация PostgreSQL:"
prompt "Имя базы данных" database false
prompt "Имя пользователя" username false
get_password password

output_file="./charts/db/values.yaml"

# Генерируем YAML файл
cat > "$output_file" <<EOF
global:
  postgresql:
    auth:
      username: "$username"
      password: "$password"
      database: "$database"
EOF

echo "Конфигурационный файл успешно создан: $output_file"