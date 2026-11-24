package main

import (
	"bytes"
	"compress/gzip"
	"context"
	"fmt"
	"log"
	"math"
	"os"
	"slices"
	"strings"

	"github.com/go-mclib/client/modules/chat"
	"github.com/go-mclib/nbt"
)

func seedLegacyData(path string) error {
	location := map[string]any{
		"level": "minecraft:overworld", "x": 20.5, "y": 100.0, "z": 24.5,
		"yRot": float32(0), "xRot": float32(0),
	}
	raw, err := nbt.Marshal(map[string]any{
		"DataVersion": int32(3955),
		"data": map[string]any{
			"schemaVersion": int32(2),
			"homes": []map[string]any{{
				"uuid": nbt.IntArray{285711214, -228312540, -1699052762, -1797257665}, "location": location,
			}},
			"warps": []map[string]any{{"name": "legacy test warp", "location": location}},
		},
	})
	if err != nil {
		return err
	}
	var compressed bytes.Buffer
	writer := gzip.NewWriter(&compressed)
	if _, err := writer.Write(raw); err != nil {
		return err
	}
	if err := writer.Close(); err != nil {
		return err
	}
	return os.WriteFile(path, compressed.Bytes(), 0600)
}

func checkLegacyData(ctx context.Context, operator *player) error {
	if err := operator.commandExpect(ctx, "home", "Teleported to your home"); err != nil {
		return err
	}
	if err := operator.waitAt(ctx, 20.5, 24.5); err != nil {
		return err
	}
	if err := operator.commandExpect(ctx, `warp tp "legacy test warp"`, "Teleported to legacy test warp"); err != nil {
		return err
	}
	log.Print("PASS 1.21.1 home and warp import")
	return nil
}

func checkRandomTeleport(ctx context.Context, operator *player) error {
	const x, z = 62.5, 36.5
	for _, command := range []string{"tp @s 62.5 100 36.5", "worldborder center 62.5 36.5", "worldborder set 10"} {
		if err := operator.SendCommand(command); err != nil {
			return err
		}
	}
	if err := operator.waitAt(ctx, x, z); err != nil {
		return err
	}
	if err := operator.commandExpect(ctx, "rtp", "Could not find a safe place to teleport"); err != nil {
		return err
	}
	if err := operator.SendCommand("worldborder set 1000"); err != nil {
		return err
	}
	if err := operator.commandExpect(ctx, "rtp", "Random teleport completed"); err != nil {
		return err
	}
	if err := operator.wait(ctx, "waiting for RTP destination within the configured distance", func() bool {
		newX, _, newZ := operator.position.Position()
		distance := math.Hypot(newX-x, newZ-z)
		return distance >= 20 && distance <= 28
	}); err != nil {
		return err
	}
	if err := operator.commandExpect(ctx, "tback", "Returned to your previous teleport location"); err != nil {
		return err
	}
	if err := operator.waitAt(ctx, x, z); err != nil {
		return err
	}
	log.Print("PASS RTP bounds, failure without cooldown, and successful teleport history")
	return nil
}

func checkSpectatorFlight(ctx context.Context, operator *player) error {
	if err := operator.SendCommand("gamemode spectator PPE_TestB"); err != nil {
		return err
	}
	if err := operator.commandExpect(ctx, "fly PPE_TestB", "Enabled flight for PPE_TestB"); err != nil {
		return err
	}
	if err := operator.commandExpect(ctx, "fly PPE_TestB", "Disabled flight for PPE_TestB"); err != nil {
		return err
	}
	if err := operator.commandExpect(ctx, "data get entity PPE_TestB abilities.mayfly", ": 1b"); err != nil {
		return err
	}
	log.Print("PASS disabling PPE flight preserves spectator flight")
	return nil
}

func checkCommandReload(ctx context.Context, operator *player, path string) error {
	config, err := os.ReadFile(path)
	if err != nil {
		return err
	}
	lines := strings.Split(string(config), "\n")
	inSpawn := false
	found := false
	for i, line := range lines {
		value := strings.TrimSpace(line)
		if strings.HasPrefix(value, "[") {
			inSpawn = value == "[commands.spawn]"
		}
		if inSpawn && value == "enabled = false" {
			lines[i] = strings.Replace(line, "false", "true", 1)
			found = true
			break
		}
	}
	if !found {
		return fmt.Errorf("missing disabled spawn fixture in %s", path)
	}

	waitForSpawn := func(want bool) error {
		return operator.wait(ctx, fmt.Sprintf("waiting for spawn availability %t", want), func() bool {
			commands, err := chat.From(operator.Client).Commands()
			return err == nil && slices.Contains(commands, "spawn") == want
		})
	}
	if err := waitForSpawn(false); err != nil {
		return err
	}
	if err := os.WriteFile(path, []byte(strings.Join(lines, "\n")), 0600); err != nil {
		return err
	}
	if err := waitForSpawn(true); err != nil {
		return err
	}
	if err := operator.commandExpect(ctx, "spawn", "Teleported to the world spawn"); err != nil {
		return err
	}
	if err := os.WriteFile(path, config, 0600); err != nil {
		return err
	}
	if err := waitForSpawn(false); err != nil {
		return err
	}
	if err := operator.commandExpect(ctx, "spawn", "Unknown or incomplete command"); err != nil {
		return err
	}
	log.Print("PASS command reload updates execution and the client command tree")
	return nil
}
