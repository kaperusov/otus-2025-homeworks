package config

import (
	"fmt"
	"log"
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
	log.Printf("Loaded config file %v", &configPath)
}
