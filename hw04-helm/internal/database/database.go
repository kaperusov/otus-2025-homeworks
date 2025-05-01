package database

import (
	"fmt"
	"log"

	"gorm.io/driver/postgres"
	"gorm.io/gorm"

	"otus/crud/internal/config"
	"otus/crud/internal/models"
)

type Database struct {
	DB *gorm.DB
}

func New() (*Database, error) {

	dsn := fmt.Sprintf("host=%s port=%d dbname=%s user=%s password=%s sslmode=disable",
		config.AppConfig.Datasource.Host,
		config.AppConfig.Datasource.Port,
		config.AppConfig.Datasource.Name,
		config.AppConfig.Datasource.Username,
		config.AppConfig.Datasource.Password,
	)

	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{})
	if err != nil {
		log.Fatal(err)
	}

	// Миграции
	db.AutoMigrate(&models.User{})

	return &Database{DB: db}, nil
}
