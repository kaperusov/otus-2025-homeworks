package database

import (
	"fmt"
	"log"
	"os"

	"gorm.io/driver/postgres"
	"gorm.io/gorm"

	"otus/crud/internal/config"
	"otus/crud/internal/models"
)

type Database struct {
	DB *gorm.DB
}

func New() (*Database, error) {

	username := config.AppConfig.Datasource.Username
	password := config.AppConfig.Datasource.Password

	if username == "" && password == "" {
		username = os.Getenv("DATASOURCE_USERNAME")
		password = os.Getenv("DATASOURCE_PASSWORD")
	}

	dsn := fmt.Sprintf("host=%s port=%d dbname=%s user=%s password=%s sslmode=disable",
		config.AppConfig.Datasource.Host,
		config.AppConfig.Datasource.Port,
		config.AppConfig.Datasource.Name,
		username,
		password,
	)

	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{})
	if err != nil {
		log.Fatal(err)
	}

	log.Printf("Connected to database: host=%s port=%d dbname=%s",
		config.AppConfig.Datasource.Host,
		config.AppConfig.Datasource.Port,
		config.AppConfig.Datasource.Name,
	)

	// Миграции
	db.AutoMigrate(&models.User{})

	return &Database{DB: db}, nil
}
