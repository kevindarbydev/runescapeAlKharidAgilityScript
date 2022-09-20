import java.awt.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;

import org.dreambot.api.Client;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.SkillTracker;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.listener.ChatListener;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.wrappers.widgets.message.Message;

import static org.dreambot.api.methods.tabs.Tabs.logout;


//2.0 Changes
// Should loot marks of grace more reliably (method for checking coords of mark -- should prevent trying to loot marks
// on previous obstacle)
// Implemented a slow mode that activates every second lap, sleep for 3-30s at various times throughout the course
//Still needs to be started in front of the course start area
//


@ScriptManifest(author = "Brotato", category = Category.AGILITY, description = "Al Kharid Agility Course", name = "Al Kharid Agility", version = 2.0)
public final class BrotatoAlKharid extends AbstractScript implements ChatListener {

    // --__--__--__--__--__--__--__--__--__--__--__--__--__--
    // __--Filters and variables_--__--__--__--__--__--__--__
    // --__--__--__--__--__--__--__--__--__--__--__--__--__--

    private final Area startArea = new Area(3269, 3200, 3279, 3195);
    private final Area longRoofArea =  new Area(3290, 3166, 3297, 3161, 3);
    private boolean slowMode;
    private int marksCollected;

    private int oldAgilityExp = 0;
    private int lapsCompleted;
    private int maxLapsCondition = 15;
    private long startTime = 0;
    @Override
    public void onStart() {
        doActionsOnStart();
    }

    @Override
    public void onExit() {
        doActionsOnExit();
    }

    @Override
    public int onLoop() {
        performLoopActions();
        return nextInt(60, 75);
    }

    @Override
    public void onPaint(Graphics g) {
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.drawString("Laps Completed: " + lapsCompleted, 12, 300);
        g.drawString("Run time: " + getElapsedTimeAsString(), 12, 240);
        g.drawString("Agility Lvl: " + Skills.getBoostedLevels(Skill.AGILITY), 12, 220);
        g.drawString("Exp/Hr: " + SkillTracker.getGainedExperiencePerHour(Skill.AGILITY), 12, 200);
        g.drawString("Time Till Level: " + makeTimeString(SkillTracker.getTimeToLevel(Skill.AGILITY)), 12, 180);
        g.drawString("Marks Collected: " + marksCollected, 12, 160);


    }



    @Override
    public void onPlayerMessage(Message msg) {
        handlePlayerMessage(msg);
    }

    @Override
    public void onMessage(Message msg) {
        handleGameMessages(msg);
    }



    private void doActionsOnStart() {
        startTime = System.currentTimeMillis();
        SkillTracker.start(Skill.AGILITY);
        oldAgilityExp = Skills.getExperience(Skill.AGILITY);
        Walking.setRunThreshold(nextInt(81, 92));

        log("Starting Brotato Script");
    }

    private void doActionsOnExit() {
        log(String.format("Gained agility xp: %d", (Skills.getExperience(Skill.AGILITY) - oldAgilityExp)));
        log("Runtime: " + getElapsedTimeAsString());
    }

    private void performLoopActions() {


        if (ScriptManager.getScriptManager().isRunning() && Client.isLoggedIn()) {
            slowMode();
            checkLapCondition();
            handleDialogues();

            checkIfWeFell();
            climbRoughWallToStart();

            crossTightrope();

            swingCable();

            zipLine();

            swingTree();

            roofTopBeams();

            finalTightrope();
            jumpGapEndCourse();

        }
    }

    // --__--__--__--__--__--__--__--__--__--__--__--__--__--
    // __--Helper functions__--__--__--__--__--__--__--__--__
    // --__--__--__--__--__--__--__--__--__--__--__--__--__--

    private void handleDialogues() {
        if (Dialogues.inDialogue()) { // see https://dreambot.org/javadocs/org/dreambot/api/methods/dialogues/Dialogues.html
            for (int i = 0; i < 4; i++) {
                if (Dialogues.canContinue()) { //
                    Dialogues.continueDialogue(); //
                    sleep(nextInt(500, 750)); //
                } else {
                    break; //break out of loop, if no more dialogues
                }
            }

        }
    }
private void checkLapCondition(){
        if (lapsCompleted >= maxLapsCondition){
            log("Max reasonable humanlike playtime reached -- logging out.");
           logout();
           stop();
        }

}
    private String getElapsedTimeAsString() {
        return makeTimeString(getElapsedTime()); //make a formatted string from a long value
    }

    private long getElapsedTime() {
        return System.currentTimeMillis() - startTime; //return elapsed millis since start of script
    }

    private String makeTimeString(long ms) {
        final int seconds = (int) (ms / 1000) % 60;
        final int minutes = (int) ((ms / (1000 * 60)) % 60);
        final int hours = (int) ((ms / (1000 * 60 * 60)) % 24);
        final int days = (int) ((ms / (1000 * 60 * 60 * 24)) % 7);
        final int weeks = (int) (ms / (1000 * 60 * 60 * 24 * 7));
        if (weeks > 0) {
            return String.format("%02dw %03dd %02dh %02dm %02ds", weeks, days, hours, minutes, seconds);
        }
        if (weeks == 0 && days > 0) {
            return String.format("%03dd %02dh %02dm %02ds", days, hours, minutes, seconds);
        }
        if (weeks == 0 && days == 0 && hours > 0) {
            return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
        }
        if (weeks == 0 && days == 0 && hours == 0 && minutes > 0) {
            return String.format("%02dm %02ds", minutes, seconds);
        }
        if (weeks == 0 && days == 0 && hours == 0 && minutes == 0) {
            return String.format("%02ds", seconds);
        }
        if (weeks == 0 && days == 0 && hours == 0 && minutes == 0 && seconds == 0) {
            return String.format("%04dms", ms);
        }
        return "00";
    }

    private void handlePlayerMessage(Message msg) {
        log(String.format("%d, %d", msg.getTime(), msg.getTypeID()));
    }

    private void handleGameMessages(Message msg) {


        log(msg);

    }
    private void slowMode(){
        slowMode = (lapsCompleted % 2 == 0 && lapsCompleted != 0);
    }


    private int nextInt(int lowValIncluded, int highValExcluded) { //get a random value between a range, high end is not included
        return ThreadLocalRandom.current().nextInt(lowValIncluded, highValExcluded);
    }

    private Player player() { //get the local player, less typing
        return getLocalPlayer();
    }

    private int playerX() { //get player x location
        return player().getX();
    }

    private int playerY() { //get player y location
        return player().getY();
    }

    private int playerZ() { //get player z location
        return player().getZ();
    }

    private boolean isMoving() { //true if player is moving
        return player().isMoving();
    }

    private boolean isAnimating() {
        return player().isAnimating();
    }

    private boolean atStartArea() { //area before the agility log
        return startArea.contains(player());
    }



    private void checkForMarks() {
        if (Inventory.isFull()) {
            log("Full inventory -- please fix");
        }
        final GroundItem mark = GroundItems.closest("Mark of Grace");
        if (mark !=null) {
            log("Found mark of grace -- sleeping briefly to ensure we loot it");
            sleep(nextInt(1500,3000));
            if (Walking.canWalk(mark))
                if (mark.interact("Take")) {
                    sleepUntil(() -> isMoving(), nextInt(500, 1000));
                    sleepUntil(() -> false, nextInt(500, 1000));
                    marksCollected++;
                }
            }
        }



    private void checkIfWeFell(){
        if (playerZ() == 0 && !atStartArea()){
            log("We fell ... attempting to restart course...");
            Walking.walk(startArea);
            sleepUntil(
                    () -> (player().distance(Walking.getDestination()) <= nextInt(3, 5)),
                    () -> isMoving(),
                    nextInt(3600, 4000), //timer duration
                    nextInt(320, 480)); //every time, poll timer is up, check reset condition. If true, then reset timer duration
                    }
    }

    private void climbRoughWallToStart() {
        if (atStartArea()) {
            log("At start area -- beginning course...");
            final GameObject rWall = GameObjects.closest(11633);
            if (rWall != null) {
                if (rWall.distance() > 9)
                    Walking.walk(rWall);


                    sleepUntil(() -> isMoving(), nextInt(500, 1000));

                }


                sleepUntil(
                        () -> (player().distance(Walking.getDestination()) <= nextInt(3, 5)),
                        () -> isMoving(),
                        nextInt(3600, 4000),
                        nextInt(320, 480));

                if (rWall.interact()) {
                    log("Found wall -- climbing...");
                    sleepUntil(
                            () -> playerZ() == 3,
                            () -> isMoving(),
                            nextInt(1000, 2000),
                            nextInt(320, 480)
                    );
                    checkForMarks();
                    log("Wall climbed -- moving to next method...");

                }
            if (slowMode){

                sleep(nextInt(6000,10000)); // Every 2nd lap, take longer breaks between obstacles
            }
            }
        }



    private void crossTightrope() {

        if (playerZ() == 3 && playerY() == 3192 && playerX() == 3273) {
            if (slowMode){
                log("slow mode true -- sleeping");
                sleep(nextInt(2000,3500));
            }
            checkForMarks();

            final GameObject tRope = GameObjects.closest(14398);
            if (tRope != null) {
                if (tRope.interact()) {
                    log("Found rope -- crossing...");

                    sleepUntil(() -> isMoving(), nextInt(500, 1000));

                    sleepUntil(
                            () -> playerY() == 3172,
                            () -> isMoving(),
                            nextInt(1000, 2000),
                            nextInt(320, 480)
                    );

                }
            }
        }

    }

    private void swingCable() {

        if (playerZ() == 3 && playerY() == 3172 && playerX() == 3272) {
            log("First tightrope succeeded...");
            checkForMarks();



            final GameObject swing = GameObjects.closest(14402);
            if (swing != null) {
                log("Attempting to swing across...");
                if (swing.interact()) {
                    sleepUntil(() -> isMoving(), nextInt(500, 1000));
                    sleepUntil(
                            () -> playerY() == 3166,
                            () -> isMoving(),
                            nextInt(1000, 2000),
                            nextInt(320, 480)
                    );
                }
                if (slowMode){
                    log("slow mode true -- sleeping");
                    sleep(nextInt(3000,22000)); // Every 2nd lap, take longer breaks between obstacles
                }
            }
        }
    }

    private void zipLine() {

        if (playerZ() == 3 && playerY() == 3166 && playerX() == 3284) {
            log("Cable swing succeeded... running to zip line.");
            checkForMarks();
            Walking.walk(longRoofArea);
            sleepUntil(() -> longRoofArea.contains(player()), nextInt(200,600));
            if (slowMode){
                log("slow mode true -- sleeping");
                sleep(nextInt(1500,3000));
            }

            final GameObject zipLine = GameObjects.closest(14403);
            if (zipLine != null) {
                log("Attempting to zip line across...");
                if (zipLine.interact()) {
                    sleepUntil(() -> isMoving(), nextInt(500, 1000));
                    sleepUntil(
                            () ->(playerZ() == 1 && playerX() == 3315),
                            () -> isMoving(),
                            nextInt(1000, 2000),
                            nextInt(320, 480)
                    );
                }
            }
        }
    }


    private void swingTree() {

        if (playerZ() == 1 && playerX() == 3315 && playerY() == 3163) {
            log("Zip Line succeeded!");
            checkForMarks();


            final GameObject treeObstacle = GameObjects.closest(14404);
            if (treeObstacle != null) {
                log("Attempting to jump over tree...");
                if (treeObstacle.interact()) {
                    sleepUntil(() -> playerY() == 3174, nextInt(3000, 3500));
                    sleepUntil(() -> isMoving(), nextInt(500, 1000));

                }
            }
        }

    }

    private void roofTopBeams() {
    //cant fail here
        if (playerZ() == 2 &&  playerY() == 3174 && playerX() == 3317) {
            log("Tree jump succeeded!");
            checkForMarks();

            final GameObject beams = GameObjects.closest(11634);
            if (beams != null) {
                log("Attempting to climb roof...");
                if (beams.interact()) {
                    sleepUntil(() -> isMoving(), nextInt(500, 1000));
                    sleepUntil(
                            () -> playerZ() == 3, //we succeeded
                            () -> (isMoving() || isAnimating()),
                            nextInt(1000, 2000),
                            nextInt(320, 480)
                    );
                }
                if (slowMode){
                    log("slow mode true -- sleeping");
                    sleep(nextInt(2000,3500)); // Every 2nd lap, take longer breaks between obstacles
                }
            }
        }
    }

    private void finalTightrope() {

        if (playerZ() == 3 && playerX() == 3316 && playerY() == 3180) {
            log("Roof climb success!");
            checkForMarks();


            final GameObject tgtrope = GameObjects.closest(14409);
            Camera.mouseRotateToEntity(tgtrope);
            if (tgtrope != null) {
                log("Attempting to use tightrope...");
                if (tgtrope.interact()) {
                    sleepUntil(() -> isMoving(), nextInt(500, 1000));
                    sleepUntil(() -> (playerX() == 3302 && playerY() == 3187), nextInt(500,1000)

                    );


                }
            }
        }
    }
    private void jumpGapEndCourse(){
        if (playerZ() == 3 && playerX() == 3302 && playerY() == 3187){
            log("Zipline success!");
            checkForMarks();
            final GameObject endCourse = GameObjects.closest(14399);
            if (endCourse != null){
                log("End of course!");
                if (endCourse.interact()){
                    sleepUntil(() -> isMoving(), nextInt(500,1000));
                    sleepUntil(() -> playerZ() == 0, nextInt(500,1000));
                    lapsCompleted++;
                }
                if (slowMode){
                    log("slow mode true -- sleeping");
                    sleep(nextInt(4000,10000)); // Every 2nd lap, take longer breaks between obstacles
                }
            }
        }
    }
}







