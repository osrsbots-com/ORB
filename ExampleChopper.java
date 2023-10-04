package me.myname.scriptname;

import com.osrsbots.orb.ORB;
import com.osrsbots.orb.api.Delay;
import com.osrsbots.orb.api.Random;
import com.osrsbots.orb.api.ScriptUtil;
import com.osrsbots.orb.api.interactables._queries.actors.query.RSPlayer;
import com.osrsbots.orb.api.interactables._queries.objects.RSObject;
import com.osrsbots.orb.api.interactables.entities.Items;
import com.osrsbots.orb.api.interactables.entities.Objects;
import com.osrsbots.orb.api.interactables.entities.Player;
import com.osrsbots.orb.scripts.Script;
import com.osrsbots.orb.scripts.ScriptMeta;

@ScriptMeta(name = "ExampleChopper", author = "ORB", version = 0.1)
public class ExampleChopper implements Script {

    /*
        Create a new enum, called State
        This is used to track the player's progress and identfy
        Which action the script should do during each loop
    */
    enum State {
        LOGIN, CHOP, IDLE, DROP
    }

    // Keep track of our set State
    State state;

    // Keep track of local player
    RSPlayer player;

    // Keep track of how many loops Player is idle
    int idles = 0;

    // Called once when script is started
    @Override
    public void onStart(String[] args) {
        // Set how often loop() is called
        ScriptUtil.setLoopDelay(1000, 2000);

        // Set beginning state
        state = State.LOGIN;
    }

    @Override
    public void loop() {
        ORB.log("@Loop~ " + state);

        switch (state) {
            case LOGIN:
                // Wait until logged in, cache reference to local player (used to check animation and see if idle)
                if (Player.isLoggedIn() && (player = Player.get()) != null) {
                    ORB.log("@LOGIN~ Ready!");
                    state = State.CHOP;
                }
                break;
            case CHOP:
                // Check if inventory is full
                if (Items.isFull()) {
                    ORB.log("@CHOP~ Inventory is full!");
                    state = State.DROP;
                    return;
                }

                // Scan game for desired object
                final RSObject tree = Objects.query().types(RSObject.Type.NORMAL).names("Tree").actions("Chop down").results().nearestToPlayer();

                // Check if we found the object
                if (tree == null) {
                    /*  TODO - Do something here, walk to the desired spot, logout, etc  */
                    ScriptUtil.stop("Failed to find tree!");
                    return;
                }

                // Highlight tree on UI

                // We DID find a tree - lets interact
                tree.interact("Chop down");

                /*
                    Script will keep looping unless we delay
                    NOTE: An improvement would be to adjust the min/max
                    time depending on a variable such as the distance of the tree
                 */
                if (Delay.untilAnimating(player, 6000, 9000)) {
                    ORB.log("@CHOP~ " + tree);

                    // Idle until tree is chopped
                    state = State.IDLE;
                } else {
                    // Failed to interact with tree
                    // NOTE: You should identify why, maybe the tree was chopped down or unreachable
                    ORB.log("@CHOP~ Failed to delay until animating!");
                }
                break;
            case IDLE:
                // Check if player is idle
                if (player.getAnimation() == -1) {

                    // Idle for a random amount of loops before chopping again
                    if (idles++ > Random.nextInt(3, 6)) {
                        ORB.log("@IDLE~ Ready!");

                        // Ready to chop another tree
                        state = State.CHOP;
                    }
                } else {
                    // Player's animation id may temporarily become -1 but player may begin animating again next frame
                    // Reset idle counter if animation is not -1
                    idles = 0;
                }

                // Idle while player is chopping...
                break;
            case DROP:
                // Drop all logs
                if (Items.drop("Logs")) {
                    state = State.IDLE;

                    ORB.log("@DROP~ Done!");
                }
                break;
        }
    }
}
