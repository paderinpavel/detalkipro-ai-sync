package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"time"

	"github.com/paderinpavel/detalkipro-emmc-agent/internal/profiles"
)

type Report struct {
	PvzID        string   `json:"pvz_id"`
	TaskID       string   `json:"task_id"`
	Chip         ChipInfo `json:"chip"`
	Operation    string   `json:"operation"`
	Mode         string   `json:"mode"`
	Hash         string   `json:"hash"`
	ChainTx      string   `json:"chain_tx"`
	Status       string   `json:"status"`
	Timestamp    string   `json:"timestamp"`
	AgentVersion string   `json:"agent_version"`
}

type ChipInfo struct {
	Model  string `json:"model"`
	Vendor string `json:"vendor"`
	Version string `json:"version"`
}

func main() {
	pvzID := flag.String("pvz-id", "", "ID пункта выдачи заказов (обязательно)")
	chipModel := flag.String("chip", "KLM8G1GETF-B041", "Модель чипа eMMC")
	profilesDir := flag.String("profiles", "profiles", "Папка с YAML-профилями")
	flag.Parse()

	if *pvzID == "" {
		fmt.Println("Ошибка: требуется --pvz-id")
		flag.Usage()
		os.Exit(1)
	}

	log.Printf("[%s] Запуск агента для ПВЗ: %s", time.Now().Format(time.RFC3339), *pvzID)

	allProfiles, err := profiles.LoadProfiles(*profilesDir)
	if err != nil {
		log.Fatalf("Не удалось загрузить профили: %v", err)
	}
	log.Printf("Загружено профилей: %d", len(allProfiles))

	profile := profiles.GetProfile(*chipModel, allProfiles)
	if !profile.IsSupported {
		log.Printf("Предупреждение: профиль для чипа %s не найден. Работаем в безопасном режиме.", profile.Model)
	}

	operation := "dump_user_area"
	mode := "read_only"
	if !profile.ReadOnly {
		mode = "unknown"
	}

	report := Report{
		PvzID:        *pvzID,
		TaskID:       fmt.Sprintf("%s-%s", *pvzID, time.Now().Format("20060102-150405")),
		Chip: ChipInfo{
			Model:  profile.Model,
			Vendor: profile.Vendor,
			Version: profile.Version,
		},
		Operation:   operation,
		Mode:        mode,
		Hash:        "sha256:placeholder_hash_for_demo",
		ChainTx:     "detalki-chain-tx-placeholder",
		Status:      "completed",
		Timestamp:   time.Now().UTC().Format(time.RFC3339),
		AgentVersion: "v0.1.0",
	}

	reportJSON, err := json.MarshalIndent(report, "", "  ")
	if err != nil {
		log.Fatalf("Ошибка формирования JSON-отчёта: %v", err)
	}

	outFile := "report.json"
	if err := os.WriteFile(outFile, reportJSON, 0644); err != nil {
		log.Fatalf("Не удалось записать отчёт: %v", err)
	}
	log.Printf("Отчёт сохранён в %s", outFile)

	fmt.Printf("\n--- Отчёт по диагностике стенда ---\n")
	fmt.Printf("ПВЗ ID:        %s\n", report.PvzID)
	fmt.Printf("Чип:           %s (%s)\n", report.Chip.Model, report.Chip.Vendor)
	fmt.Printf("Версия eMMC:    %s\n", report.Chip.Version)
	fmt.Printf("USER область:   %d МБ\n", profile.UserSizeMB)
	fmt.Printf("Режим:         %s\n", mode)
	fmt.Printf("Статус:        %s\n", report.Status)
	fmt.Printf("Hash:          %s\n", report.Hash)
	fmt.Printf("Chain Tx:      %s\n", report.ChainTx)
	fmt.Printf("-----------------------------------\n")

	fmt.Println("\n[ПЛАН ОПЕРАЦИИ]")
	fmt.Println("- Чтение extCSD (спецификация чипа)")
	fmt.Println("- Снятие дампа USER-области (только чтение)")
	fmt.Println("- Хеширование (SHA‑256) и фиксация в DetalkiPro Chain")
	fmt.Println("- Формирование отчёта и архива")
}
