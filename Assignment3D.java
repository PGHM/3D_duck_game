package graffa;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.core.PMatrix3D;
import processing.core.PVector;
import saito.objloader.OBJModel;
import cg.Vector3f;
import ddf.minim.AudioPlayer;
import ddf.minim.Minim;

public class Assignment3D extends PApplet {

    // if you don't wan't sounds played, change this
    static final boolean SOUNDS = true;

    // if you wan't to explore the place without fear of being killed, this is your boolean to falsify
    static final boolean SKULL_ENABLED = true;
    
    /* containers */
    CopyOnWriteArrayList<Bomb> projectiles = new CopyOnWriteArrayList<Assignment3D.Bomb>();
    CopyOnWriteArrayList<Explosion> explosions = new CopyOnWriteArrayList<Assignment3D.Explosion>();
    ArrayList<PVector> duckPositions = new ArrayList<PVector>();
    ArrayList<Wall> walls = new ArrayList<Wall>();
    
    /* constants */
    final int roomSize = 50;
    final int roomTileSize = 10;
    final int jumpHeight = 9;
    final int viewHeight = 10;
    final int startPointX = 15;
    final int startPointZ = 35;
    final float hoveringSpeed = 0.15f;
    final float hoveringAltitude = 2.4f;
    final float playerRadius = 3f;
    final float startFacing = QUARTER_PI;
    final float skullStartX = 75;
    final float skullStartZ = -25;
    final float wallCollideDistance = 2;
    final FlyCam cam = new FlyCam();
    
    /* this can be changed to add more difficulty */
    final float skullSpeed= 1.2f;
    
    /* variables */
    boolean looking = false;
    boolean jumpingUp = false;
    boolean jumpingDown = false;
    boolean playerDead = false;
    public boolean playerWon = false;

    int redValue = 128;
    int greenValue = 128;
    int blueValue = 128;
    
    int score = 0;
    Random random = new Random();
    float dyingEffectAngle = 0;
    float timeDead = 0;
    
    /* textures */
    PImage lava;
    PImage wall;
    PImage water;
    PImage snow;
    PImage lava2;
    PImage stars2;
    PImage grass;
    PImage desert;
    PImage ice;
    PImage wood;
    PImage leaf;
    
    /* objects */
    OBJModel tree;
    OBJModel duckModel;
    Duck duck;
    Skull skull;
    
    /* sounds */
    Minim minim;
    AudioPlayer squack;
    AudioPlayer victory;
    AudioPlayer explosion;
    AudioPlayer shoot;
    AudioPlayer death;
    AudioPlayer music;

    public void setup() {

        frameRate(30);
        size(960, 720, OPENGL);
        textureMode(NORMALIZED);
        
        /* init textures and sounds */
        lava = loadImage("lava.jpg");
        wall = loadImage("wall.jpg");
        ice = loadImage("ice.jpg");
        water = loadImage("water.jpg");
        lava2 = loadImage("lava2.jpg");
        stars2 = loadImage("stars2.jpg");
        grass = loadImage("grass.jpg");
        desert = loadImage("desert.jpg");
        wood = loadImage("wood.jpg");
        leaf = loadImage("leaf.jpg");
        snow = loadImage("snow.jpg");

        minim = new Minim(this);
        squack = minim.loadFile("duck.wav");
        victory = minim.loadFile("duck_victory.wav");
        explosion = minim.loadFile("explosion2.wav");
        shoot = minim.loadFile("shoot.wav");
        death = minim.loadFile("death.wav");
        music = minim.loadFile("music.mp3");

        if(SOUNDS) {
            music.loop();
        }

        /* duck route, this can be changed to your liking :) */

        duckPositions.add(new PVector(75, random.nextInt(9) + 10,-25));
        duckPositions.add(new PVector(25, random.nextInt(9) + 10, 25));
        duckPositions.add(new PVector(75, random.nextInt(9) + 10, 25));
        duckPositions.add(new PVector(25, random.nextInt(9) + 10, -25));
        duckPositions.add(new PVector(75, random.nextInt(9) + 10, 35));

        /* init objects */
        tree = new OBJModel(this, "baobab_tree.obj", QUADS);
        tree.scale(3.5f, -5f, 3.5f);
        tree.enableMaterial();
        tree.enableTexture();
        
        duckModel = new OBJModel(this,"RubberDucky.obj", QUADS);
        duckModel.scale(1f, -1f, 1f);
        duckModel.enableMaterial();
        duckModel.enableTexture();

        PVector firstPosition = duckPositions.remove(0);
        duck = new Duck(firstPosition.x, firstPosition.y, firstPosition.z, random.nextFloat() * 2.0f * PI, 0, duckModel);
        skull = new Skull(skullStartX, 10, skullStartZ, -HALF_PI, skullSpeed);
        
        /* inti font for the victory and death screen */
        PFont myFont = createFont("MV Boli", 32);
        textFont(myFont);
        textAlign(CENTER);
        
        /* set the starting positiong and facing */
        cam.pos.set(startPointX, viewHeight, startPointZ);
        cam.phi = startFacing;
        
        /* set invisible collision test wall objects to the textures */
        makeWallsForTextures(roomSize, roomTileSize, new boolean[] { true, true, false, false }, 0, 0);
        makeWallsForTextures(roomSize, roomTileSize, new boolean[] { true, false, false, true }, 50.1f, 0);
        makeWallsForTextures(roomSize, roomTileSize, new boolean[] { false, true, true, false }, 0, -50.1f);
        makeWallsForTextures(roomSize, roomTileSize, new boolean[] { false, false, true, true }, 50.1f, -50.1f);
    }


    public void draw() {
        
        /* if player won, show the winning screen and play the victory squack! */
        if(playerWon) {
            background(255,255,0);
            camera();
            fill(255,0,0);
            text("You got all the the ducks so you WIN <3" , 240, 320, 500, 200);
            return;
        }
        
        /* if player is dead, switch to death animation and after a while show the death message. If alive, just update the camera */
        if(!playerDead) {
            updateCamera();
        } else {
            timeDead++;
            if(timeDead > 100) {
                background(0);
                camera();
                fill(255,0,0);
                text("You managed to get " + score + " ducks! You needed " + (duckPositions.size() + 1) + " more :(" , 240, 320, 500, 200);
                return;
            } else {
                updateWhileDead();
            }
        }
        
        perspective(40.0f * PI / 180.0f, 4.0f / 3.0f, 0.1f, 1000.0f);

        /* Make sure that the camera matrix is identity. (This was from the assignment, I don't know exactly what it does
         * but the program won't work without it, so it stays...
         */
        beginCamera();
        resetMatrix();
        scale(1, -1, 1);
        endCamera();
        resetMatrix();

        /* Flip y-axis up. Using a left handed coordinate system is insane.waa. - Agreed! */
        scale(1, -1, 1);
        
        /* if the player is dead dim the lights to red and then black, otherwise normal lights*/
        if(!playerDead) {
            lights();
        } else {
            lightsWhileDead();
        }

        /* Set viewer position (= Camera matrix) */
        cam.apply();
        
        /* check if character is jumping and update camera accordingly */
        jump();
        
        /* draw scenery */
        background(0);
        scene();
    }

    void scene() {
        
        /* draw all the objects first */
        for(Bomb p : projectiles) {
            p.draw();
        }

        for(Explosion e : explosions) {
            e.draw();
        }

        if(SKULL_ENABLED) {
            skull.draw();
        }

        duck.draw();

        pushMatrix();
        translate(90, 0, 40);
        tree.draw();
        popMatrix();
        
        /* then switch texturemode, otherwise the textures won't work properly, the object loader changes it back everytime it draws
         * an object so this has to be done every time
         */
        textureMode(NORMALIZED);
        
        /* draw walls, roof and floor, all made from textured tiles */
        pushMatrix();
        translate(0, 0, 0);
        textureCubeWithDoors(lava, wall, stars2, roomSize, roomTileSize, new boolean[] { true, true, false, false });
        popMatrix();
        pushMatrix();
        translate(50.1f, 0, 0);
        textureCubeWithDoors(grass, wood, leaf, roomSize, roomTileSize, new boolean[] { true, false, false, true });
        popMatrix();
        pushMatrix();
        translate(0, 0, -50.1f);
        textureCubeWithDoors(snow, water, ice, roomSize, roomTileSize, new boolean[] { false, true, true, false });
        popMatrix();
        pushMatrix();
        translate(50.1f, 0, -50.1f);
        textureCubeWithDoors(lava2, desert, wall, roomSize, roomTileSize, new boolean[] { false, false, true, true });
        popMatrix();
    }
    
    boolean checkCollisions(float x, float z) {
        for (Wall wall : walls) {
            if (wall.isColliding(x, z)) {
                return true;
            }
        }
        return false;
    }
    
    /* when dying, the character falls and head tilts */
    void updateWhileDead() {
        if(cam.pos.v[1] > 1) {
            cam.pos.v[1] -= 0.3f;
        }
        if(dyingEffectAngle < HALF_PI) {
            dyingEffectAngle += PI/64;
        }
        cam.apply();
    }
    
    void lightsWhileDead() {
        /* normal lights() function except with custom colour values */
        ambientLight(redValue, greenValue, blueValue);
        directionalLight(redValue, greenValue, blueValue, 0, 0, -1);
        lightFalloff(1, 0, 0);
        lightSpecular(0, 0, 0);

        if(timeDead < 75) {
            if(redValue < 230) redValue++;
            if(greenValue > 20) greenValue--;
            if(blueValue > 20) blueValue--;
        } else {
            if(redValue > 0) redValue = redValue - 10;
            if(greenValue > 0) greenValue--;
            if(blueValue > 0) blueValue--;
        }
    }
    
    void jump() {
        if (jumpingUp) {
            cam.pos.v[1] += 0.8f;
            if (cam.pos.v[1] >= viewHeight + jumpHeight) {
                jumpingUp = false;
                jumpingDown = true;
            }
        } else if (jumpingDown) {
            cam.pos.v[1] -= 0.8f;
            if (cam.pos.v[1] <= viewHeight) {
                jumpingDown = false;
            }
        }
        
    }

    public void keyPressed() {
        if (key == 'a')
            cam.move_left = true;
        if (key == 'd')
            cam.move_right = true;
        if (key == 'w')
            cam.move_forward = true;
        if (key == 's')
            cam.move_backward = true;
        if (key == ' ') {
            jumpingUp = true;
        }
    }

    public void keyReleased() {
        if (key == 'a')
            cam.move_left = false;
        if (key == 'd')
            cam.move_right = false;
        if (key == 'w')
            cam.move_forward = false;
        if (key == 's')
            cam.move_backward = false;
    }

    public void mousePressed() {
        if(!playerDead) {
            noCursor();
            looking = true;
        }
    }

    public void mouseReleased() {
        looking = false;
        cursor();
    }

    public void mouseMoved() {
        if (looking) {
            panCamera();
        }
    }

    public void mouseDragged() {
        if (looking) {
            panCamera();
        }
    }
    
    /* pretty much the model implementation from the labyrinth assignment */
    void panCamera() {
        float rads_per_pixel = (2.0f * PI * 40.0f / 360.0f) / 480.0f;
        int dx = mouseX - pmouseX;
        int dy = mouseY - pmouseY;
        /* Pan camera around using mouse. */
        cam.phi += dx * 5 * rads_per_pixel;
        cam.theta += dy * 5 * rads_per_pixel;
        /* Limit vertical angle to avoid upside-down position. */
        cam.theta = min(cam.theta, 0.5f * PI);
        cam.theta = max(cam.theta, -0.5f * PI);
    }

    void updateCamera() {
        
        Vector3f p = cam.pos.clone();
        cam.update();
        
        float x = cam.pos.x();
        float z = cam.pos.z();
        
        /* if the character is trying to go through wall, stop him */
        if (checkCollisions(x, z)) {
            cam.pos = p;
        }
    }
    
    /*
     * Size must be dividable by tileSize The door order is: North, East, South,
     * West (North is the direction character is first looking without facing adjustments
     */

    public void textureCubeWithDoors(PImage floorTexture, PImage wallTexture,
        PImage roofTexture, int size, int tileSize, boolean[] doors) {
        
        noStroke();
        beginShape(QUADS);
        texture(wallTexture);
        int tiles = size / tileSize;
        
        /* the loops simply make larger square from smaller ones, and leave a doorway in the middle
         *  if there is supposed to be one
         */
        for (int i = 0; i < tiles; i++) {
            for (int j = 0; j < tiles; j++) {
                if (doors[0] && j < 2 && (i == tiles / 2)) {
                    // Making doorway!
                    continue;
                }
                vertex(tileSize * i, tileSize * j, 0, 0, 0);
                vertex(tileSize * (i + 1), tileSize * j, 0, tileSize, 0);
                vertex(tileSize * (i + 1), tileSize * (j + 1), 0, tileSize,
                    tileSize);
                vertex(tileSize * i, tileSize * (j + 1), 0, 0, tileSize);
            }
        }

        for (int i = 0; i < tiles; i++) {
            for (int j = 0; j < tiles; j++) {
                if (doors[1] && i < 2 && (j == tiles / 2)) {
                    // Making doorway!
                    continue;
                }
                vertex(size, tileSize * i, tileSize * (j + 1), 0, 0);
                vertex(size, tileSize * i, tileSize * j, tileSize, 0);
                vertex(size, tileSize * (i + 1), tileSize * j, tileSize, tileSize);
                vertex(size, tileSize * (i + 1), tileSize * (j + 1), 0, tileSize);
            }
        }

        for (int i = 0; i < tiles; i++) {
            for (int j = 0; j < tiles; j++) {
                if (doors[2] && j < 2 && (i == tiles / 2)) {
                    // Making doorway!
                    continue;
                }
                vertex(tileSize * (i + 1), tileSize * j, size, 0, 0);
                vertex(tileSize * i, tileSize * j, size, tileSize, 0);
                vertex(tileSize * i, tileSize * (j + 1), size, tileSize, tileSize);
                vertex(tileSize * (i + 1), tileSize * (j + 1), size, 0, tileSize);
            }
        }

        for (int i = 0; i < tiles; i++) {
            for (int j = 0; j < tiles; j++) {
                if (doors[3] && i < 2 && (j == tiles / 2)) {
                    // Making doorway!
                    continue;
                }
                vertex(0, tileSize * i, tileSize * j, 0, 0);
                vertex(0, tileSize * i, tileSize * (j + 1), tileSize, 0);
                vertex(0, tileSize * (i + 1), tileSize * (j + 1), tileSize, tileSize);
                vertex(0, tileSize * (i + 1), tileSize * j, 0, tileSize);
            }
        }
        
        /* no doorways in roof and floor, otherwise the same */
        endShape();
        noStroke();
        beginShape(QUADS);
        texture(floorTexture);
        for (int i = 0; i < tiles; i++) {
            for (int j = 0; j < tiles; j++) {
                vertex(tileSize * i, 0, tileSize * (j + 1), 0, 0);
                vertex(tileSize * (i + 1), 0, tileSize * (j + 1), tileSize, 0);
                vertex(tileSize * (i + 1), 0, tileSize * j, tileSize, tileSize);
                vertex(tileSize * i, 0, tileSize * j, 0, tileSize);
            }
        }
        endShape();
        noStroke();
        beginShape(QUADS);
        texture(roofTexture);
        for (int i = 0; i < tiles; i++) {
            for (int j = 0; j < tiles; j++) {
                vertex(tileSize * i, size, tileSize * (j + 1), 0, 0);
                vertex(tileSize * (i + 1), size, tileSize * (j + 1), tileSize, 0);
                vertex(tileSize * (i + 1), size, tileSize * j, tileSize, tileSize);
                vertex(tileSize * i, size, tileSize * j, 0, tileSize);
            }
        }
        endShape();
    }
    
     /* Xoffset and Zoffset determine the place of the cube */
    public void makeWallsForTextures(int size, int tileSize, boolean[] doors, float Xoffset, float Zoffset) {

        int tiles = size / tileSize;
        
        for (int i = 0; i < tiles; i++) {
            if (doors[0] && i == tiles / 2) {
                continue;
            }
            walls.add(new Wall(Xoffset + i * tileSize, Zoffset, Xoffset + (i + 1) * tileSize, Zoffset));
        }

        for (int i = 0; i < tiles; i++) {
            if (doors[1] && i == tiles / 2) {
                continue;
            }
            walls.add(new Wall(size + Xoffset, Zoffset + i * tileSize, size + Xoffset, Zoffset + (i + 1) * tileSize));
        }

        for (int i = 0; i < tiles; i++) {
            if (doors[2] && i == tiles / 2) {
                continue;
            }
            walls.add(new Wall(Xoffset + i * tileSize, Zoffset + size, Xoffset + (i + 1) * tileSize, Zoffset + size));
        }

        for (int i = 0; i < tiles; i++) {
            if (doors[3] && i == tiles / 2) {
                continue;
            }
            walls.add(new Wall(Xoffset, Zoffset + i * tileSize, Xoffset, Zoffset + (i + 1) * tileSize));
        }
    }
    
    /* pretty much the model implementation from the labyrinth assignment */
    class FlyCam {

        final float speed = 0.7f;
        boolean move_forward;
        boolean move_backward;
        boolean move_left;
        boolean move_right;
        Vector3f pos; // Viewer position
        float phi; // Horizontal angle
        float theta; // Vertical angle
        PMatrix3D view; // Viewing matrix

        FlyCam() {
            view = new PMatrix3D(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
            pos = new Vector3f(0, 0, 0);
            move_forward = false;
            move_backward = false;
            move_left = false;
            move_right = false;
        }

        /* Update camera position using current motion state. */
        void update() {
            float dx = 0.0f;
            float dz = 0.0f;
            dx += move_left ? 1.0 : 0.0;
            dx += move_right ? -1.0 : 0.0;
            dz += move_forward ? 1.0 : 0.0;
            dz += move_backward ? -1.0 : 0.0;
            /* Viewing directions are in the view matrix rows as viewer X,Z */
            pos.v[0] -= speed * (dx * view.m00 + dz * view.m20);
            pos.v[2] -= speed * (dx * view.m02 + dz * view.m22);
        }

        /* Apply viewing matrix corresponding to camera. */
        void apply() {
            /* Form viewing matrix from angles and position. */
            view.set(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
            view.rotateX(theta);
            view.rotateY(phi);
            if(playerDead) {
                view.rotateZ(dyingEffectAngle );
            }
            view.translate(-pos.x(), -pos.y(), -pos.z());
            applyMatrix(view);
        }
    }

    class Wall {

        float startZ;
        float startX;
        float endX;
        float endZ;

        /* Make wall by giving a line when X or Z coordinates are the same, or give four distinct values that limit the wall borders */
        public Wall(float startX, float startZ, float endX, float endZ) {
            if (startZ == endZ) {
                this.startZ = startZ - wallCollideDistance;
                this.endZ = startZ + wallCollideDistance;
                this.startX = startX;
                this.endX = endX;
            } else if(startX == endX) {
                this.startZ = startZ;
                this.endZ = endZ;
                this.startX = startX - wallCollideDistance;
                this.endX = startX + wallCollideDistance;
            } else {
                this.startZ = startZ - wallCollideDistance;
                this.endZ = endZ + wallCollideDistance;
                this.startX = startX - wallCollideDistance;
                this.endX = endX + wallCollideDistance;
            }
        }

        public boolean isColliding(float x, float y) {
            return x < endX && x > startX && y < endZ && y > startZ;
        }

        public String toString() {
            return "StartX :" + startX + "\nEndX: " + endX + "\nStartZ: " + startZ + "\nEndZ: " + endZ + "\n";
        }
    }
    
    class FloatingObject {

        float posX;
        float posY;
        float posZ;
        float facing;
        float speed;
        OBJModel object;

        public FloatingObject(float x, float y, float z, float facing, float speed, OBJModel model) {
            posX = x;
            posY = y;
            posZ = z;
            this.facing = facing;
            this.speed = speed;
            object = model;
        }
        
        /* move the object to the direction it is facing the amount of speed */
        public void draw() {
            posX += speed * Math.sin(facing);
            posZ += speed * Math.cos(facing);
            pushMatrix();
            translate(posX, posY, posZ);
            rotateY(facing);
            object.draw();
            popMatrix();
        }
    }
    
    /* enemy of this game, it shoots bob-ombs and if one hits you, you die */
    class Skull extends FloatingObject {

        final int shootDistance = 33;
        final float shootArc = PI/64;
        final float turnSpeed = PI/64;
        
        float shootDelay = 60;
        float upLimit;
        float downLimit;
        boolean goingUp = true;
        boolean turning = false;
        float previousFacing;
        

        public Skull(float x, float y, float z, float facing, float speed) {
            super(x,y,z,facing,speed, new OBJModel(Assignment3D.this, "Skull.obj"));
            object.scale(0.01f, -0.01f, 0.01f);
            upLimit = y + hoveringAltitude;
            downLimit = y - hoveringAltitude;
        }

        @Override
        public void draw() {
            /* make the skull hover a bit, looks more scary! */
            if(goingUp) {
                posY += hoveringSpeed;
                if(posY >= upLimit) {
                    goingUp = false;
                }
            } else {
                posY -= hoveringSpeed;
                if(posY <= downLimit) {
                    goingUp = true;
                }
            }

            /* look for target three times per frame, this makes the target acquisition faster */
            lookForTarget();
            lookForTarget();
            lookForTarget();
            
            /* dont move if the skull is turning and searching target, just rotate */
            if(turning) {
                pushMatrix();
                translate(posX, posY, posZ);
                rotateY(facing);
                object.draw();
                popMatrix();
            } else {
                /* Skull patrols through the area */
                if(posX >= 75 && posZ >= 25)
                    facing = PI;
                else if(posX >= 75 && posZ <= -25)
                    facing = -HALF_PI;
                else if(posX <= 25 && posZ <= -25)
                    facing = 0;
                else if(posX <= 25 && posZ >= 25)
                    facing = HALF_PI;
                super.draw();
            }
        }

        public void lookForTarget() {
            
            /* if the skull has shot, count to 60 before it can shoot again */
            if(shootDelay == 0) {
                shootDelay = 60;
            } else if (shootDelay < 60) {
                shootDelay--;
            }

            float distanceToPlayer = (float)Math.sqrt(Math.pow(posX - cam.pos.v[0], 2) + Math.pow(posZ - cam.pos.v[2], 2));
            
            /* return the facing angle to PI .. -PI */
            float normalizedFacing = facing;
            while(normalizedFacing < -PI)
                normalizedFacing += 2 * PI;
            while(normalizedFacing >= PI)
                normalizedFacing -= 2* PI;
            
            /* if skull is in a shooting distance, put it in turning mode and save the direction it was going to */
            if(distanceToPlayer <= shootDistance && !turning) {
                turning = true;
                previousFacing = facing;
                System.out.println("Starting to turn");
                
            /* search for player and shoot if the player is in your sights */
            } else if (distanceToPlayer <= shootDistance && turning) {
                float relativeFacingToPlayer = (float)Math.atan2(cam.pos.v[0] - posX, cam.pos.v[2] - posZ);
                float difference = relativeFacingToPlayer - normalizedFacing;

                if (Math.abs(difference) <= shootArc) {
                    if (shootDelay == 60) {
                        shoot();
                        shootDelay--;
                    }
                    
                /* depending on the result of the difference in the angles, turn the skull */
                } else if ((difference < 0 && difference >= -PI) || difference >= PI) {
                    facing -= turnSpeed;
                } else {
                    facing += turnSpeed;
                }
                
            /* if player is too far go back to normal patrol route */
            } else if (distanceToPlayer > shootDistance && turning) {
                float differenceToPreviousFacing = previousFacing - normalizedFacing;
                if(Math.abs(differenceToPreviousFacing) <=  shootArc) {
                    facing = previousFacing;
                    turning = false;
                }
                else if (differenceToPreviousFacing < 0 || differenceToPreviousFacing > PI) {
                    facing -= turnSpeed;
                } else {
                    facing += turnSpeed;
                }
            }
        }
        
        /* try to shoot the player, make a new bomb projectile with triple speed compared to the skull */
        public void shoot() {
            if(!playerDead && SOUNDS) {
                shoot.rewind();
                shoot.play();
            }
            OBJModel bomb = new OBJModel(Assignment3D.this, "bomberman.obj", QUADS);
            bomb.scale(0.03f, -0.03f, 0.03f);
            new Bomb(posX, posY, posZ, facing, speed*3, bomb);
        }
    }

    class Bomb extends FloatingObject {

        public Bomb(float x, float y, float z, float direction, float speed, OBJModel model) {
            super(x,y,z,direction,speed, model);
            projectiles.add(this);
        }

        @Override
        public void draw() {
            float distanceToPlayer = (float)Math.sqrt(Math.pow(posX - cam.pos.v[0], 2) + Math.pow(posZ - cam.pos.v[2], 2));
            
            /* if the bomb hits the wall, it explodes, logical yes? */
            if (checkCollisions(posX, posZ)) {
                projectiles.remove(this);
                new Explosion(posX, posY, posZ);
                return;
            }
            
            /* if it hits the player, the player dies, again logical? */
            if(distanceToPlayer < playerRadius) {
                projectiles.remove(this);
                new Explosion(posX, posY, posZ);
                playerDead = true;
                if(SOUNDS) {
                    death.play();
                }
                return;
            }

            posX += speed * Math.sin(facing);
            posZ += speed * Math.cos(facing);
            pushMatrix();
            translate(posX, posY, posZ);
            rotateY(facing);

            /* for some reason the bombs leave the skull in a slightly off X-axis */
            translate(-2.2f, 0, 0);
            object.draw();
            popMatrix();
        }
    }

    class Duck extends FloatingObject {
        public Duck(float x, float y, float z, float direction, float speed, OBJModel model) {
            super(x,y,z,direction,speed, model);
        }

        @Override
        public void draw() {
            float distanceToPlayer = (float)Math.sqrt(Math.pow(posX - cam.pos.v[0], 2) + Math.pow(posZ - cam.pos.v[2], 2));
            
            /* check if the player is in the right spot */
            if(distanceToPlayer < playerRadius) {
                
                /* and in the right height */
                if(cam.pos.v[1] - posY > 0) {
                    score++;
                    if(SOUNDS) {
                        squack.rewind();
                        squack.play();
                    }
                    
                    /* check win */
                    if(duckPositions.size() == 0) {
                        playerWon = true;
                        if(SOUNDS) {
                            music.pause();
                            victory.play();
                        }
                        return;
                    }
                    PVector duckPosition = duckPositions.remove(0);
                    duck = new Duck(duckPosition.x, duckPosition.y, duckPosition.z, random.nextFloat() * 2.0f * PI, 0, duckModel);
                }
            }
            super.draw();
        }
    }

    class Explosion {

        float posX;
        float posY;
        float posZ;

        float radius = 1f;
        float maxRadius = 3;

        public Explosion(float x, float y, float z) {
            posX = x;
            posY = y;
            posZ = z;
            explosions.add(this);
            if(!playerDead && SOUNDS) {
                explosion.rewind();
                explosion.play();
            }
        }

        public void draw() {

            pushMatrix();
            translate(posX, posY, posZ);
            fill(255,0,0);
            sphere(radius);
            popMatrix();
            radius += 0.3f;
            if(radius >= maxRadius) {
                explosions.remove(this);
            }
        }
    }
}
