package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.joints.HingeJoint;
import com.jme3.bullet.joints.Point2PointJoint;
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.bullet.joints.SliderJoint;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 * @author normenhansen
 */
public class Main extends SimpleApplication {

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
    
    /** Prepare the Physics Application State (jBullet) */
    private BulletAppState bulletAppState;

    /** Prepare Materials */
    private Material wall_mat;
    private Material stone_mat;
    private Material floor_mat;

    /** Prepare geometries and physical nodes for walls, floor and cannon balls. */
    private static final Box    wall;
    private RigidBodyControl    ball_phy;
    private static final Sphere sphere;
    private RigidBodyControl    floor_phy;
    private static final Box    floor;
    
    private static final float  FLOOR_SIDE_LEN = 600f;
    private static final float  WALL_HEIGHT = 200f;
    
    /** Physics-based character control */
    private CharacterControl player;
    private final Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false, run = false;
    
    //Temporary vectors used on each frame.
    //They here to avoid instanciating new vectors on each frame
    private final Vector3f camDir = new Vector3f();
    private final Vector3f camLeft = new Vector3f();
    
    static {
        /** Initialize the cannon ball geometry */
        sphere = new Sphere(32, 32, 0.8f, true, false);
        sphere.setTextureMode(TextureMode.Projected);
        /** Initialize the brick geometry */
        wall = new Box(FLOOR_SIDE_LEN, WALL_HEIGHT, 0.1f);
        wall.scaleTextureCoordinates(new Vector2f(10f, 5f));
        /** Initialize the floor geometry */
        floor = new Box(FLOOR_SIDE_LEN, 0.1f, FLOOR_SIDE_LEN);
        floor.scaleTextureCoordinates(new Vector2f(30, 40));
    }
    
    /** Initialize the materials used in this scene. */
    private void initMaterials() {
        wall_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key = new TextureKey("Textures/Terrain/BrickWall/BrickWall.jpg");
        key.setGenerateMips(true);
        Texture tex = assetManager.loadTexture(key);
        tex.setWrap(WrapMode.Repeat);
        wall_mat.setTexture("ColorMap", tex);

        stone_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key2 = new TextureKey("Textures/Terrain/Rock/Rock.PNG");
        key2.setGenerateMips(true);
        Texture tex2 = assetManager.loadTexture(key2);
        tex2.setWrap(WrapMode.Repeat);
        stone_mat.setTexture("ColorMap", tex2);

        floor_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key3 = new TextureKey("Textures/Terrain/Pond/Pond.jpg");
        key3.setGenerateMips(true);
        Texture tex3 = assetManager.loadTexture(key3);
        tex3.setWrap(WrapMode.Repeat);
        floor_mat.setTexture("ColorMap", tex3);
    }

    /** Make a solid floor and add it to the scene. */
    private void initFloor() {
        Geometry floor_geo = new Geometry("Floor", floor);
        floor_geo.setMaterial(floor_mat);
        floor_geo.setLocalTranslation(0, -WALL_HEIGHT/2, 0);
        rootNode.attachChild(floor_geo);
        /* Make the floor physical with mass 0.0f! */
        floor_phy = new RigidBodyControl(0.0f);
        floor_geo.addControl(floor_phy);
        bulletAppState.getPhysicsSpace().add(floor_phy);
    }
    
    /** Make 4 walls, one for each side of the floor. */
    private void initWalls() {
        RigidBodyControl wall_phy;
        Geometry wall1 = new Geometry("Wall 1", wall);
        wall1.setMaterial(wall_mat);
        wall1.setLocalTranslation(0, 0, -FLOOR_SIDE_LEN);
        rootNode.attachChild(wall1);
        wall_phy = new RigidBodyControl(0.0f);
        wall1.addControl(wall_phy);
        bulletAppState.getPhysicsSpace().add(wall_phy);
        
        Geometry wall2 = new Geometry("Wall 2", wall);
        wall2.setMaterial(wall_mat);
        wall2.setLocalTranslation(0, 0, FLOOR_SIDE_LEN);
        rootNode.attachChild(wall2);
        wall_phy = new RigidBodyControl(0.0f);
        wall2.addControl(wall_phy);
        bulletAppState.getPhysicsSpace().add(wall_phy);
        
        Geometry wall3 = new Geometry("Wall 3", wall);
        wall3.setMaterial(wall_mat);
        wall3.setLocalTranslation(-FLOOR_SIDE_LEN, 0, 0);
        wall3.rotate(0, (float) (Math.PI/2), 0);
        rootNode.attachChild(wall3);
        wall_phy = new RigidBodyControl(0.0f);
        wall3.addControl(wall_phy);
        bulletAppState.getPhysicsSpace().add(wall_phy);
        
        Geometry wall4 = new Geometry("Wall 4", wall);
        wall4.setMaterial(wall_mat);
        wall4.setLocalTranslation(FLOOR_SIDE_LEN, 0, 0);
        wall4.rotate(0, (float) (Math.PI/2), 0);
        rootNode.attachChild(wall4);
        wall_phy = new RigidBodyControl(0.0f);
        wall4.addControl(wall_phy);
        bulletAppState.getPhysicsSpace().add(wall_phy);
    }
    
    private void initScaffold() {
        Box scaffold = new Box(10f, 5f, 60f);
        scaffold.scaleTextureCoordinates(new Vector2f(5f, 2f));
        Geometry sc_geom = new Geometry("Scaffold", scaffold);
        sc_geom.setMaterial(stone_mat);
        sc_geom.setLocalTranslation(0, 9f - WALL_HEIGHT/2, 0);
        rootNode.attachChild(sc_geom);
        RigidBodyControl sc_phy = new RigidBodyControl(0);
        sc_geom.addControl(sc_phy);
        bulletAppState.getPhysicsSpace().add(sc_phy);
        
        Box step = new Box(10f, 1f, 5f);
        RigidBodyControl step_phy;
        
        Geometry step1 = new Geometry("Step 1", step);
        step1.setMaterial(stone_mat);
        step1.setLocalTranslation(0, 1f - WALL_HEIGHT/2, -80f);
        rootNode.attachChild(step1);
        step_phy = new RigidBodyControl(0);
        step1.addControl(step_phy);
        bulletAppState.getPhysicsSpace().add(step_phy);
        
        Geometry step2 = new Geometry("Step 2", step);
        step2.setMaterial(stone_mat);
        step2.setLocalTranslation(0, 3f - WALL_HEIGHT/2, -77f);
        rootNode.attachChild(step2);
        step_phy = new RigidBodyControl(0);
        step2.addControl(step_phy);
        bulletAppState.getPhysicsSpace().add(step_phy);
        
        Geometry step3 = new Geometry("Step 3", step);
        step3.setMaterial(stone_mat);
        step3.setLocalTranslation(0, 5f - WALL_HEIGHT/2, -74f);
        rootNode.attachChild(step3);
        step_phy = new RigidBodyControl(0);
        step3.addControl(step_phy);
        bulletAppState.getPhysicsSpace().add(step_phy);
        
        Geometry step4 = new Geometry("Step 4", step);
        step4.setMaterial(stone_mat);
        step4.setLocalTranslation(0, 7f - WALL_HEIGHT/2, -71f);
        rootNode.attachChild(step4);
        step_phy = new RigidBodyControl(0);
        step4.addControl(step_phy);
        bulletAppState.getPhysicsSpace().add(step_phy);
        
        Geometry step5 = new Geometry("Step 5", step);
        step5.setMaterial(stone_mat);
        step5.setLocalTranslation(0, 9f - WALL_HEIGHT/2, -68f);
        rootNode.attachChild(step5);
        step_phy = new RigidBodyControl(0);
        step5.addControl(step_phy);
        bulletAppState.getPhysicsSpace().add(step_phy);
        
        Geometry step6 = new Geometry("Step 6", step);
        step6.setMaterial(stone_mat);
        step6.setLocalTranslation(0, 11f - WALL_HEIGHT/2, -65f);
        rootNode.attachChild(step6);
        step_phy = new RigidBodyControl(0);
        step6.addControl(step_phy);
        bulletAppState.getPhysicsSpace().add(step_phy);
    }
    
    private void initObstacles() {
        // The hookNode is the fixed point from which the bag hangs. It has no mass.
//        Node hookNode = new Node("Hook");
//        hookNode.setLocalTranslation(new Vector3f(0, 40f - WALL_HEIGHT/2, 0));
//        BoxCollisionShape hook = new BoxCollisionShape(new Vector3f( .1f, .1f, .1f));
//        RigidBodyControl hook_phy = new RigidBodyControl(hook, 0);
//        hook_phy.setPhysicsLocation(new Vector3f(0, 40f - WALL_HEIGHT/2, 0));
//        hookNode.addControl(hook_phy);
//        rootNode.attachChild(hookNode);
//        bulletAppState.getPhysicsSpace().add(hook_phy);
        
        // The cylinder represents a dynamic bag that has mass.
        Cylinder bag_shape = new Cylinder(6, 18, 6f, 20f, true);
        Geometry bag = new Geometry("Bag", bag_shape);
        bag.setMaterial(stone_mat);
        bag.setLocalTranslation(0, 30f - WALL_HEIGHT/2, 0);
        bag.rotate((float) (Math.PI/2), 0, 0);
        RigidBodyControl bag_phy = new RigidBodyControl(50f);
        bag.addControl(bag_phy);
//        bag_phy.applyImpulse(new Vector3f(15f, 0, 0), Vector3f.ZERO);
        bag.addControl(new BagMotionControl());
        rootNode.attachChild(bag);
        bulletAppState.getPhysicsSpace().add(bag_phy);
        bag_phy.setGravity(Vector3f.ZERO);
        
        // The link between them.
//        Point2PointJoint joint = new Point2PointJoint(hook_phy, bag_phy, new Vector3f(0, -1f, 0), new Vector3f(0, 0, -10f));
//        HingeJoint joint = new HingeJoint(hook_phy, bag_phy, new Vector3f(0, 0, 0), new Vector3f(0, 0, -10f), Vector3f.UNIT_Z, Vector3f.UNIT_Z);
//        SixDofJoint joint = new SixDofJoint(hook_phy, bag_phy, new Vector3f(0, 0, 0), new Vector3f(0, 0, -10f), true);
//        joint.setLinearLowerLimit(Vector3f.ZERO);
//        joint.setLinearUpperLimit(Vector3f.ZERO);
//        SliderJoint joint = new SliderJoint(hook_phy, bag_phy, new Vector3f(0, 0, 0), new Vector3f(0, 0, -10f), false);
//        joint.setLowerLinLimit(0);
//        joint.setUpperLinLimit(0);
//        bulletAppState.getPhysicsSpace().add(joint);
    }
    
    /** A plus sign used as crosshairs to help the player with aiming.*/
    private void initCrossHairs() {
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+");        // fake crosshairs :)
        ch.setLocalTranslation( // center
          settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
          settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }

    @Override
    public void simpleInitApp() {
        /** Set up Physics Game */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
//        bulletAppState.setDebugEnabled(true);

        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        cam.setAxes(Vector3f.UNIT_X, Vector3f.UNIT_Y, cam.getDirection().negate());
        cam.setFrustumFar(1500f);
        flyCam.setMoveSpeed(10f);
        
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(2f, 10f, 1);
        player = new CharacterControl(capsuleShape, 2f);
        player.setJumpSpeed(20);
        player.setFallSpeed(30);
        player.setGravity(new Vector3f(0, -30f, 0));
        player.setPhysicsLocation(new Vector3f(20f, -WALL_HEIGHT/2 + 12f, -120f));
        bulletAppState.getPhysicsSpace().add(player);
        
        setUpKeys();
        /** Add InputManager action: Left click triggers shooting. */
        inputManager.addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "shoot");
        /** Initialize the scene, materials, and physics space */
        initMaterials();
        initWalls();
        initFloor();
        initScaffold();
        initObstacles();
        initCrossHairs();
    }
    
    /** We over-write some navigational key mappings here, so we can
     * add physics-controlled walking and jumping: */
    private void setUpKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Run", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE), new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(keysListener, "Left");
        inputManager.addListener(keysListener, "Right");
        inputManager.addListener(keysListener, "Up");
        inputManager.addListener(keysListener, "Down");
        inputManager.addListener(keysListener, "Run");
        inputManager.addListener(keysListener, "Jump");
    }
    
    private final ActionListener keysListener = new ActionListener() {
        @Override
        public void onAction(String binding, boolean isPressed, float tpf) {
            switch (binding) {
                case "Left":
                    left = isPressed;
                    break;
                case "Right":
                    right= isPressed;
                    break;
                case "Up":
                    up = isPressed;
                    break;
                case "Down":
                    down = isPressed;
                    break;
                case "Run":
                    run = isPressed;
                    break;
                case "Jump":
                    if (isPressed) player.jump(new Vector3f(0, 20f, 0));
                    break;
                default:
                    break;
            }
        }
    };
    
    /**
     * Every time the shoot action is triggered, a new cannon ball is produced.
     * The ball is set up to fly from the camera position in the camera direction.
     */
    private ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("shoot") && !keyPressed) {
                makeCannonBall();
            }
        }
    };
    
    /** This method creates one individual physical cannon ball.
     * By defaul, the ball is accelerated and flies
     * from the camera position in the camera direction.*/
    private void makeCannonBall() {
        /** Create a cannon ball geometry and attach to scene graph. */
        Geometry ball_geo = new Geometry("cannon ball", sphere);
        ball_geo.setMaterial(stone_mat);
        rootNode.attachChild(ball_geo);
        /** Position the cannon ball  */
        ball_geo.setLocalTranslation(cam.getLocation());
        /** Make the ball physcial with a mass > 0.0f */
        ball_phy = new RigidBodyControl(10f);
        /** Add physical ball to physics space. */
        ball_geo.addControl(ball_phy);
        bulletAppState.getPhysicsSpace().add(ball_phy);
        /** Accelerate the physcial ball to shoot it. */
        ball_phy.setLinearVelocity(cam.getDirection().mult(60));
    }

    @Override
    public void simpleUpdate(float tpf) {
        camDir.set(cam.getDirection()).multLocal(0.6f);
        camLeft.set(cam.getLeft()).multLocal(0.4f);
        walkDirection.set(0, 0, 0);
        
        if (left) walkDirection.addLocal(camLeft);
        if (right) walkDirection.addLocal(camLeft.negate());
        if (up) walkDirection.addLocal(camDir);
        if (down) walkDirection.addLocal(camDir.negate());
        if (run) walkDirection.multLocal(2f);
        
        player.setWalkDirection(walkDirection);
        cam.setLocation(player.getPhysicsLocation());
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}

class BagMotionControl extends AbstractControl {    
    private static final double AMPLITUDE = 20.0;
    
    private float time = 0;
    private float x0 = 0;
    private Quaternion rot0;
    private RigidBodyControl phys;
    private Vector3f pos;
    private final Quaternion rot = new Quaternion();

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        phys = spatial.getControl(RigidBodyControl.class);
        pos = phys.getPhysicsLocation();
        x0 = pos.getX();
        rot0 = phys.getPhysicsRotation();
    }

    @Override
    protected void controlUpdate(float tpf) {
        time += tpf;
        if (spatial != null) {
            float x = (float) (AMPLITUDE * Math.cos(2*time));
            pos.setX(x0 + x);
            phys.setPhysicsLocation(pos);
            float a = (float) (0.3 * Math.cos(2*time));
            rot.fromAngleAxis(a, Vector3f.UNIT_Y); // rotation in local coordinates
            phys.setPhysicsRotation(rot0.mult(rot));
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // not needed
    }
}
