package swagger

import (
	"encoding/json"
	"log"
	"net/http"
	"sync/atomic"
)

var isReady atomic.Bool

// Health godoc
// @Summary Проверка состояния
// @Description Отвеает, жив ли контейнер
// @Tags Service state
// @Accept json
// @Produce json
// @Success 200 "Сервис жив"
// @Failure 500 "Ошибка сервера"
// @Router /health [get]
func Health(w http.ResponseWriter, r *http.Request) {
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

// Ready godoc
// @Summary Проверка готовности к работе
// @Description Отвечает, готов ли контейнер принимать трафик
// @Tags Service state
// @Accept json
// @Produce json
// @Success 200 "Сервис готов к работе"
// @Failure 500 "Ошибка сервера"
// @Router /ready [get]
func Ready(w http.ResponseWriter, r *http.Request) {
	if !isReady.Load() {
		w.WriteHeader(http.StatusServiceUnavailable)
		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(map[string]string{"status": "NOT_READY"})
		return
	}

	response := map[string]string{"status": "READY"}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(response); err != nil {
		log.Printf("healthHandler: failed to encode JSON response: %v", err)
	}
}
