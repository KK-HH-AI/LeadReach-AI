package handler

import (
	"github.com/gin-gonic/gin"

	"github.com/KK-HH-AI/LeadReach-AI/gateway-service/internal/config"
)

// Handler holds application dependencies.
type Handler struct {
	cfg *config.Config
}

// New creates a new Handler.
func New(cfg *config.Config) *Handler {
	return &Handler{cfg: cfg}
}

// healthResponse is the health check response body.
type healthResponse struct {
	Status string `json:"status"`
	App    string `json:"app"`
	Env    string `json:"env"`
}

func (h *Handler) Health(c *gin.Context) {
	c.JSON(200, healthResponse{
		Status: "ok",
		App:    h.cfg.App.Name,
		Env:    h.cfg.App.Env,
	})
}
