package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"math"
	"net"
	"net/netip"
	"os"
	"regexp"
	"strings"
	"time"

	"github.com/go-mclib/client"
	"github.com/go-mclib/client/botcli"
	"github.com/go-mclib/client/modules/chat"
	"github.com/go-mclib/client/modules/inventory"
	"github.com/go-mclib/client/modules/self"
	"github.com/go-mclib/data/items"
	"github.com/go-mclib/data/packets"
	ns "github.com/go-mclib/protocol/javaproto/wire"
)

var legacyFormatting = regexp.MustCompile(`§[0-9a-fk-or]`)

type player struct {
	*client.Client
	inventory *inventory.Module
	position  *self.Module
	messages  chan string
	done      chan error
}

func join(ctx context.Context, address, username string) (*player, error) {
	c := botcli.NewClient(botcli.Flags{
		Address: address, Username: username, TreatTransferAsDisconnect: true,
	})
	c.Register(inventory.New())
	p := &player{c, inventory.From(c), self.From(c), make(chan string, 64), make(chan error, 1)}
	spawned := make(chan struct{}, 1)
	p.position.OnSpawn(func() {
		select {
		case spawned <- struct{}{}:
		default:
		}
	})
	chat.From(c).OnSystemChat(func(message string, overlay bool) {
		if !overlay {
			select {
			case p.messages <- message:
			default:
			}
		}
	})
	go func() { p.done <- c.ConnectAndStart(ctx) }()
	select {
	case <-spawned:
		return p, nil
	case err := <-p.done:
		return nil, fmt.Errorf("%s disconnected before spawning: %v", username, err)
	case <-ctx.Done():
		return nil, fmt.Errorf("waiting for %s to spawn: %w", username, ctx.Err())
	}
}

func (p *player) wait(ctx context.Context, label string, satisfied func() bool) error {
	ticker := time.NewTicker(50 * time.Millisecond)
	defer ticker.Stop()
	for !satisfied() {
		select {
		case err := <-p.done:
			return fmt.Errorf("%s disconnected while %s: %v", p.Username, label, err)
		case <-ctx.Done():
			return fmt.Errorf("%s: %w", label, ctx.Err())
		case <-ticker.C:
		}
	}
	return nil
}

func (p *player) expectMessage(ctx context.Context, message string) error {
	return p.wait(ctx, "waiting for "+p.Username+" to receive "+message, func() bool {
		for {
			select {
			case received := <-p.messages:
				if strings.Contains(legacyFormatting.ReplaceAllString(received, ""), message) {
					return true
				}
			default:
				return false
			}
		}
	})
}

func (p *player) waitAt(ctx context.Context, x, z float64) error {
	return p.wait(ctx, fmt.Sprintf("waiting for %s at X/Z %.1f, %.1f", p.Username, x, z), func() bool {
		actualX, _, actualZ := p.position.Position()
		return math.Abs(actualX-x) < 0.1 && math.Abs(actualZ-z) < 0.1
	})
}

func (p *player) selectTarget(ctx context.Context, command, title string) error {
	if err := p.SendCommand(command); err != nil {
		return err
	}
	var menu inventory.Snapshot
	err := p.wait(ctx, "waiting for "+title+" with a player in the first slot", func() bool {
		menu = p.inventory.Snapshot()
		return menu.WindowID != 0 && menu.Ready && menu.MenuType == inventory.MenuGeneric9x6 &&
			strings.Contains(menu.Title, title) && menu.ContainerSlots == 54 && !menu.Slots[0].IsEmpty()
	})
	if err != nil {
		return err
	}
	head := ns.VarInt(items.ID("minecraft:player_head"))
	if menu.Slots[45].ItemID != head || menu.Slots[53].ItemID != head {
		return fmt.Errorf("missing skull-based navigation buttons in %s", title)
	}
	// PPE handles the slot as a button, so a normal click leaves the carried item empty.
	err = p.SendPacket(&packets.C2SContainerClick{
		WindowID: ns.VarInt(menu.WindowID), StateID: ns.VarInt(menu.StateID), Slot: 0,
		CarriedItem: ns.EmptyHashedSlot(),
	})
	if err != nil {
		return err
	}
	return p.wait(ctx, "waiting for menu to close after selecting the player", func() bool {
		return !p.inventory.ContainerOpen()
	})
}

func run(ctx context.Context, address string) error {
	requester, err := join(ctx, address, "PPE_TestA")
	if err != nil {
		return err
	}
	target, err := join(ctx, address, "PPE_TestB")
	if err != nil {
		return err
	}

	const requesterX, targetX, z = 32.5, 48.5, 32.5
	for _, command := range []string{
		"tp PPE_TestA 32.5 100 32.5", "tp PPE_TestB 48.5 100 32.5",
	} {
		if err := requester.SendCommand(command); err != nil {
			return err
		}
	}
	if err := requester.waitAt(ctx, requesterX, z); err != nil {
		return fmt.Errorf("positioning test players (PPE_TestA must be an operator): %w", err)
	}
	if err := target.waitAt(ctx, targetX, z); err != nil {
		return err
	}

	if err := requester.selectTarget(ctx, "tpa", "TPA: To Player"); err != nil {
		return err
	}
	if err := target.expectMessage(ctx, "PPE_TestA wants to teleport to your location"); err != nil {
		return err
	}
	if err := target.commandExpect(ctx, "tpaa", "You accepted PPE_TestA's TPA request"); err != nil {
		return err
	}
	if err := requester.expectMessage(ctx, "PPE_TestB accepted your TPA request"); err != nil {
		return err
	}
	if err := requester.waitAt(ctx, targetX, z); err != nil {
		return err
	}
	log.Print("PASS /tpa menu, request, acceptance, and requester teleport")

	if err := requester.SendCommand("tp PPE_TestA 32.5 100 32.5"); err != nil {
		return err
	}
	if err := requester.waitAt(ctx, requesterX, z); err != nil {
		return err
	}
	if err := requester.selectTarget(ctx, "tpahere", "TPA: Invite"); err != nil {
		return err
	}
	if err := target.expectMessage(ctx, "PPE_TestA wants you to teleport to their location"); err != nil {
		return err
	}
	if err := target.SendCommand("tpaherea"); err != nil {
		return err
	}
	if err := target.waitAt(ctx, requesterX, z); err != nil {
		return err
	}
	log.Print("PASS /tpahere menu, request, acceptance, and target teleport")
	return checkCommands(ctx, requester, target)
}

func (p *player) commandExpect(ctx context.Context, command, message string) error {
	if err := p.SendCommand(command); err != nil {
		return err
	}
	return p.expectMessage(ctx, message)
}

func checkCommands(ctx context.Context, operator, executor *player) error {
	if err := operator.commandExpect(ctx, "heal PPE_TestB", "Healed PPE_TestB"); err != nil {
		return err
	}
	if err := executor.expectMessage(ctx, "You were healed by PPE_TestA"); err != nil {
		return err
	}
	if err := operator.commandExpect(ctx, "execute as PPE_TestB run heal", "Healed PPE_TestB"); err != nil {
		return err
	}
	if err := executor.commandExpect(ctx, "execute as PPE_TestA run tellraw PPE_TestB "+fmt.Sprintf("%q", "execute check ready"), "execute check ready"); err != nil {
		return err
	}
	if err := executor.SendCommand("execute as PPE_TestA run god"); err != nil {
		return err
	}
	if err := executor.commandExpect(ctx, "tellraw @s "+fmt.Sprintf("%q", "permission check completed"), "permission check completed"); err != nil {
		return err
	}
	if err := operator.commandExpect(ctx, "god", "Enabled god mode for PPE_TestA"); err != nil {
		return err
	}
	if err := operator.commandExpect(ctx, "god", "Disabled god mode for PPE_TestA"); err != nil {
		return err
	}
	log.Print("PASS native execute context, sender feedback, and sender permissions")

	if err := operator.SendCommand("execute as PPE_TestB positioned 60.5 100 36.5 run repeat 1 tp @s ~ ~ ~"); err != nil {
		return err
	}
	if err := executor.waitAt(ctx, 60.5, 36.5); err != nil {
		return err
	}
	if err := operator.SendCommand("execute as PPE_TestB run home set"); err != nil {
		return err
	}
	if err := operator.SendCommand("tp PPE_TestB 80.5 100 36.5"); err != nil {
		return err
	}
	if err := executor.waitAt(ctx, 80.5, 36.5); err != nil {
		return err
	}
	if err := operator.SendCommand("execute as PPE_TestB run home tp"); err != nil {
		return err
	}
	if err := executor.waitAt(ctx, 60.5, 36.5); err != nil {
		return err
	}
	if err := operator.SendCommand("tp @s 62.5 100 36.5"); err != nil {
		return err
	}
	if err := operator.waitAt(ctx, 62.5, 36.5); err != nil {
		return err
	}
	if err := operator.SendCommand("execute as PPE_TestB run repeat 1"); err != nil {
		return err
	}
	if err := executor.waitAt(ctx, 62.5, 36.5); err != nil {
		return err
	}
	log.Print("PASS repeat preserves executor and position, uses the sender's history, and grouped home commands use the executor's home")

	const warpName = "主城 广场"
	quoted := fmt.Sprintf("%q", warpName)
	if err := operator.commandExpect(ctx, "warp set "+quoted, "Set warp "+warpName); err != nil {
		return err
	}
	reply, err := chat.From(operator.Client).SuggestCommandContext(ctx, nil, "/warp tp 主")
	if err != nil {
		return err
	}
	found := false
	for _, suggestion := range reply.Matches {
		found = found || string(suggestion.Text) == quoted
	}
	if !found {
		return fmt.Errorf("missing quoted Unicode warp completion: %v", reply.Matches)
	}
	if err := operator.commandExpect(ctx, "warp tp "+warpName, "Teleported to "+warpName); err != nil {
		return err
	}
	if err := operator.commandExpect(ctx, "warp delete "+quoted, "Deleted warp "+warpName); err != nil {
		return err
	}
	log.Print("PASS Unicode warp creation, quoted Tab completion, bare teleport, and quoted deletion")
	return nil
}

func main() {
	address := flag.String("address", "127.0.0.1:25565", "disposable offline-mode 26.3 test server")
	timeout := flag.Duration("timeout", time.Minute, "total test timeout")
	flag.Parse()
	host, _, err := net.SplitHostPort(*address)
	ip, parseErr := netip.ParseAddr(host)
	if err != nil || parseErr != nil || !ip.IsLoopback() {
		log.Fatal("address must use a loopback IP and port")
	}
	ctx, cancel := context.WithTimeout(context.Background(), *timeout)
	err = run(ctx, *address)
	cancel()
	if err != nil {
		log.Print(err)
		os.Exit(1)
	}
}
