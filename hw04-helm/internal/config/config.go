package config

import (
	"fmt"
	"log"
	"net/http"
	"os"

	"gopkg.in/yaml.v3"
)

type Config struct {
	Datasource struct {
		Host     string `yaml:"host"`
		Port     int    `yaml:"port"`
		Name     string `yaml:"name"`
		Username string `yaml:"username"`
		Password string `yaml:"password"`
	}
}

var AppConfig *Config

func LoadConfig(configPath string) {

	file, err := os.Open(configPath)
	if err != nil {
		panic(fmt.Sprintf("Can't open config file: %v", err))
	}
	defer file.Close()

	decoder := yaml.NewDecoder(file)
	AppConfig = &Config{}

	if err := decoder.Decode(AppConfig); err != nil {
		panic(fmt.Sprintf("Failed while read config file: %v", err))
	}
	log.Printf("Config successfylly loaded")
}

func EnableCORS(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Разрешаем запросы с любого origin (*)
		w.Header().Set("Access-Control-Allow-Origin", "*")
		// Разрешаем методы GET, POST, OPTIONS и т.д.
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
		// Разрешаем необходимые заголовки
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type")

		// Если это OPTIONS-запрос (preflight), завершаем его успешно
		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusOK)
			return
		}

		log.Printf("CORS enabled")
		// Передаем запрос дальше
		next.ServeHTTP(w, r)
	})
}
