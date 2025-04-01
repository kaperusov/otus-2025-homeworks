package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os"
	"time"
)

func healthHandler(w http.ResponseWriter, r *http.Request) {
	startTime := time.Now()

	log.Printf("Received request: %s %s", r.Method, r.URL.Path)

	if r.Method != http.MethodGet {
		w.WriteHeader(http.StatusMethodNotAllowed)
		log.Printf("Method not allowed: %s", r.Method)
		return
	}

	response := map[string]string{"status": "OK"}
	w.Header().Set("Content-Type", "application/json")

	if err := json.NewEncoder(w).Encode(response); err != nil {
		log.Printf("Failed to encode response: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	duration := time.Since(startTime)
	log.Printf("Request processed successfully in %v", duration)
}

func main() {
	// Настройка логгера
	log.SetOutput(os.Stdout)
	log.SetFlags(log.Ldate | log.Ltime | log.Lshortfile)
	log.Println("Starting server on :8000")

	http.HandleFunc("/health", healthHandler)

	if err := http.ListenAndServe(":8000", nil); err != nil {
		log.Fatalf("Server failed: %v", err)
	}
}
