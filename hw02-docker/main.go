package main

import (
	"encoding/json"
	"flag"
	"log"
	"net/http"
	"os"
	"sync/atomic"
	"time"

	"github.com/gorilla/mux"
)

var isReady atomic.Bool

func healthHandler(w http.ResponseWriter, r *http.Request) {
	startTime := beginRequest(r.Method, r.URL.Path)
	defer endRequest(startTime, r.Method, r.URL.Path)

	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		log.Printf("healthHandler: unsupported method %s", r.Method)
		return
	}

	response := map[string]string{"status": "OK"}
	w.Header().Set("Content-Type", "application/json")

	if err := json.NewEncoder(w).Encode(response); err != nil {
		log.Printf("healthHandler: failed to encode JSON response: %v", err)
	}
}

func helloHandler(w http.ResponseWriter, r *http.Request) {
	startTime := beginRequest(r.Method, r.URL.Path)
	defer endRequest(startTime, r.Method, r.URL.Path)

	vars := mux.Vars(r)
	name := vars["name"]

	response := map[string]string{"hello": name}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

func readyHandler(w http.ResponseWriter, r *http.Request) {
	startTime := beginRequest(r.Method, r.URL.Path)
	defer endRequest(startTime, r.Method, r.URL.Path)

	if !isReady.Load() {
		w.WriteHeader(http.StatusServiceUnavailable)
		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(map[string]string{"status": "NOT_READY"})
		return
	}

	response := map[string]string{"status": "READY"}
	// Если в HTTP заголовке передали имя студента, добавляем его в ответ
	if studentName := r.Header.Get("X-Student-Name"); studentName != "" {
		response["student"] = studentName
	}

	w.WriteHeader(http.StatusOK)
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(response)
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
	// Определяем флаг (--port или -p)
	port := flag.String("port", "8000", "Port to run the server on")
	flag.Parse()

	// эмулируем инициализацию
	go func() {
		time.Sleep(5 * time.Second) // имитируем загрузку
		isReady.Store(true)
	}()

	// Настройка логгера
	log.SetOutput(os.Stdout)
	log.SetFlags(log.Ldate | log.Ltime | log.Lshortfile)

	r := mux.NewRouter()
	r.HandleFunc("/health", healthHandler).Methods("GET")
	r.HandleFunc("/ready", readyHandler).Methods("GET")
	r.HandleFunc("/hello/{name}", helloHandler).Methods("GET")

	log.Printf("Server started at :%v", *port)
	log.Fatal(http.ListenAndServe(":"+*port, r))
}
