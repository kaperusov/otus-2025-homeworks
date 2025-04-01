package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/gorilla/mux"
)

func healthHandler(w http.ResponseWriter, r *http.Request) {
	startTime := beginRequest(r.Method, r.URL.Path)

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

	endRequest(startTime, r.Method, r.URL.Path)
}

func helloHandler(w http.ResponseWriter, r *http.Request) {
	startTime := beginRequest(r.Method, r.URL.Path)

	vars := mux.Vars(r)
	name := vars["name"]

	response := map[string]string{"hello": name}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)

	endRequest(startTime, r.Method, r.URL.Path)
}

func beginRequest(method string, path string) time.Time {
	startTime := time.Now()
	log.Printf("Received request: %s %s", method, path)
	return startTime
}

func endRequest(startTime time.Time, method string, path string) {
	duration := time.Since(startTime)
	log.Printf("Request %s %s processed successfully in %v", method, path, duration)
}

func main() {
	// Настройка логгера
	log.SetOutput(os.Stdout)
	log.SetFlags(log.Ldate | log.Ltime | log.Lshortfile)

	r := mux.NewRouter()
	r.HandleFunc("/health", healthHandler).Methods("GET")
	r.HandleFunc("/hello/{name}", helloHandler).Methods("GET")

	log.Println("Server started at :8000")
	log.Fatal(http.ListenAndServe(":8000", r))
}
