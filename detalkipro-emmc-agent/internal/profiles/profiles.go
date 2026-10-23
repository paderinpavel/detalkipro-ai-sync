package profiles

import (
	"fmt"
	"io/fs"
	"os"
	"path/filepath"

	"gopkg.in/yaml.v3"
)

type EmmcProfile struct {
	Vendor      string `yaml:"vendor"`
	Model       string `yaml:"model"`
	Version     string `yaml:"version"`
	UserSizeMB  int64  `yaml:"user_size_mb"`
	Boot0SizeMB int64  `yaml:"boot0_size_mb"`
	Boot1SizeMB int64  `yaml:"boot1_size_mb"`
	RPMBSizeMB  int64  `yaml:"rmpb_size_mb"`
	IsSupported bool   `yaml:"supported"`
	ReadOnly    bool   `yaml:"read_only"`
	Notes       string `yaml:"notes"`
}

func LoadProfiles(dir string) ([]*EmmcProfile, error) {
	var profiles []*EmmcProfile

	err := filepath.WalkDir(dir, func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if d.IsDir() {
			return nil
		}
		ext := filepath.Ext(d.Name())
		if ext != ".yaml" && ext != ".yml" {
			return nil
		}

		data, err := os.ReadFile(path)
		if err != nil {
			return fmt.Errorf("не удалось прочитать профиль %s: %w", path, err)
		}

		var p EmmcProfile
		if err := yaml.Unmarshal(data, &p); err != nil {
			return fmt.Errorf("ошибка парсинга YAML %s: %w", path, err)
		}

		if !p.IsSupported {
			return nil
		}

		profiles = append(profiles, &p)
		return nil
	})
	if err != nil {
		return nil, err
	}

	return profiles, nil
}

func GetProfile(model string, allProfiles []*EmmcProfile) *EmmcProfile {
	for _, p := range allProfiles {
		if p.Model == model {
			return p
		}
	}
	return &EmmcProfile{
		Vendor:      "Unknown",
		Model:       model,
		Version:     "",
		UserSizeMB:  0,
		Boot0SizeMB: 0,
		Boot1SizeMB: 0,
		RPMBSizeMB:  0,
		IsSupported: false,
		ReadOnly:    true,
		Notes:       "Профиль не найден. Безопасный режим.",
	}
}
