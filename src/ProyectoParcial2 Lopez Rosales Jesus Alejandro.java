import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;

class GameChuy extends JFrame implements KeyListener, MouseMotionListener, MouseListener {
    private final ArrayList<Shape> baseShapes;
    private final ArrayList<Bullet> bullets;
    private final ArrayList<Bullet> enemyBullets;
    private final Timer timer;
    private final Random random;
    private double rotation = 0;

    private PlayerShip player;
    private EnemyMandala boss;
    private Point mousePosition;

    private Clip backgroundMusic;
    private Clip shootSound;
    private Clip explosionSound;
    private Clip gameOverSound;
    private Clip victorySound;

    private boolean gameOver = false;

    public GameChuy() {
        setTitle("GameChuy");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        initializeCursor();

        initializeSounds();

        baseShapes = new ArrayList<>();
        bullets = new ArrayList<>();
        enemyBullets = new ArrayList<>();
        random = new Random();
        player = new PlayerShip(50, 550);
        boss = new EnemyMandala(750, 50);
        int CENTER_X = 400;
        int CENTER_Y = 300;
        mousePosition = new Point(CENTER_X, CENTER_Y);

        initializeShapes();

        timer = new Timer(10, _ -> {
            rotation += 0.02;
            player.update();
            boss.update();
            updateBullets();
            checkCollisions();
            repaint();
        });
        timer.start();

        playBackgroundMusic();

        add(new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                float hue1 = (float) ((Math.sin(System.currentTimeMillis() * 0.0001) + 1) / 2) * 0.2f;
                float hue2 = (float) ((Math.cos(System.currentTimeMillis() * 0.00015) + 1) / 2) * 0.2f + 0.6f;

                Color color1 = Color.getHSBColor(hue1, 0.8f, 0.2f);
                Color color2 = Color.getHSBColor(hue2, 0.8f, 0.2f);

                GradientPaint gradient = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2, true);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                g2d.setColor(Color.WHITE);
                for (int i = 0; i < 100; i++) {
                    int x = random.nextInt(getWidth());
                    int y = random.nextInt(getHeight());
                    g2d.fillOval(x, y, 2, 2);
                }

                drawMandala(g2d);
                player.draw(g2d, mousePosition);
                drawBullets(g2d);
                drawHealthBars(g2d);
            }
        });


        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
        setFocusable(true);
    }

    private void initializeCursor() {
        BufferedImage cursorImg = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = cursorImg.createGraphics();

        // Dibujar la mira
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(8, 8, 16, 16);
        g2d.drawLine(16, 4, 16, 12);
        g2d.drawLine(16, 20, 16, 28);
        g2d.drawLine(4, 16, 12, 16);
        g2d.drawLine(20, 16, 28, 16);

        g2d.dispose();

        // Crear y establecer el cursor personalizado
        Cursor customCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(16, 16), "Custom Cursor");
        setCursor(customCursor);
    }

    private void initializeSounds() {
        try {
            backgroundMusic = loadSound("background.wav");
            shootSound = loadSound("shoot.wav");
            explosionSound = loadSound("explosion.wav");
            gameOverSound = loadSound("gameover.wav");
            victorySound = loadSound("victory.wav");
        } catch (Exception e) {
            System.err.println("Error loading sounds: " + e.getMessage());
        }
    }

    private Clip loadSound(String filename) {
        try {
            File soundFile = new File("src/sound/" + filename);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            return clip;
        } catch (Exception e) {
            System.err.println("Error loading sound " + filename + ": " + e.getMessage());
            return null;
        }
    }

    private void playBackgroundMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    private void playSound(Clip sound) {
        if (sound != null) {
            sound.setFramePosition(0);
            sound.start();
        }
    }

    private void checkGameOver() {
        if (!gameOver) {
            if (player.health <= 0) {
                gameOver = true;
                stopGame();
                playSound(gameOverSound);
                showGameOverDialog(false);
            } else if (boss.health <= 0) {
                gameOver = true;
                stopGame();
                playSound(victorySound);
                showGameOverDialog(true);
            }
        }
    }

    private void stopGame() {
        timer.stop();
        backgroundMusic.stop();
    }

    private void showGameOverDialog(boolean victory) {
        SwingUtilities.invokeLater(() -> {
            String message = victory ?
                    "¡Felicidades! Has derrotado al jefe.\n¿Quieres jugar de nuevo?" :
                    "Game Over\n¿Quieres intentarlo de nuevo?";

            int option = JOptionPane.showConfirmDialog(
                    this,
                    message,
                    "Fin del juego",
                    JOptionPane.YES_NO_OPTION
            );

            if (option == JOptionPane.YES_OPTION) {
                restartGame();
            } else {
                JOptionPane.showMessageDialog(this, "¡Hasta luego!");
                System.exit(0);
            }
        });
    }

    private void restartGame() {
        // Reiniciar variables del juego
        player = new PlayerShip(50, 550);
        boss = new EnemyMandala(750, 50);
        bullets.clear();
        enemyBullets.clear();
        rotation = 0;
        gameOver = false;
        timer.start();

        // Reiniciar música
        backgroundMusic.setFramePosition(0);
        playBackgroundMusic();
    }


    private void initializeShapes() {
        addBaseShapes();
    }

    private void addBaseShapes() {
        int[] xPoints = {0, -10, 10};
        int[] yPoints = {-30, -15, -15};
        baseShapes.add(new Polygon(xPoints, yPoints, 3));
        baseShapes.add(new Rectangle2D.Double(-25, -60, 50, 20));
        baseShapes.add(new Ellipse2D.Double(-8, -80, 16, 16));
        baseShapes.add(new Rectangle2D.Double(-5, -100, 10, 30));
        baseShapes.add(new Ellipse2D.Double(-20, -20, 40, 40));
        baseShapes.add(new Ellipse2D.Double(-15, -15, 30, 30));
    }

    private void drawMandala(Graphics2D g2d) {
        AffineTransform originalTransform = g2d.getTransform();
        g2d.translate(boss.x, boss.y);

        for (int layer = 0; layer < 3; layer++) {
            double layerRotation = rotation * (layer % 2 == 0 ? 1 : -1);
            int SEGMENTS = 12;
            for (int segment = 0; segment < SEGMENTS; segment++) {
                double angle = (2 * Math.PI * segment / SEGMENTS) + layerRotation;
                for (Shape baseShape : baseShapes) {
                    AffineTransform transform = new AffineTransform();
                    transform.rotate(angle);
                    transform.scale(1 + layer * 0.5, 1 + layer * 0.5);
                    double shearX = Math.sin(rotation) * 0.5;
                    double shearY = Math.cos(rotation) * 0.5;
                    transform.shear(shearX, shearY);
                    double scale = 1 + Math.sin(rotation) * 0.1;
                    transform.scale(scale, scale);
                    Shape transformedShape = transform.createTransformedShape(baseShape);
                    float hue = (float) ((angle + rotation) / (2 * Math.PI));
                    g2d.setColor(Color.getHSBColor(hue, 0.8f, 0.9f));
                    g2d.fill(transformedShape);
                    g2d.setColor(new Color(0, 0, 0, 100));
                    g2d.draw(transformedShape);
                }
            }
        }
        g2d.setTransform(originalTransform);
    }

    private void updateBullets() {
        for (int i = 0; i < bullets.size(); i++) {
            Bullet bullet = bullets.get(i);
            bullet.update();
            if (bullet.isOffScreen(getWidth(), getHeight())) {
                bullets.remove(i);
                i--;
            }
        }
        for (int j = 0; j < enemyBullets.size(); j++) {
            Bullet bullet = enemyBullets.get(j);
            bullet.update();
            if (bullet.isOffScreen(getWidth(), getHeight())) {
                enemyBullets.remove(j);
                j--;
            }
        }
    }

    private void drawBullets(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        for (Bullet bullet : bullets) {
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(bullet.x, bullet.y, bullet.x + bullet.dx * 2, bullet.y + bullet.dy * 2);
        }

        float hue = (float) ((rotation + Math.PI) / (2 * Math.PI));
        g2d.setColor(Color.getHSBColor(hue, 0.8f, 0.9f));
        for (Bullet bullet : enemyBullets) {
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(bullet.x, bullet.y, bullet.x + bullet.dx * 2, bullet.y + bullet.dy * 2);
        }
    }

    private void checkCollisions() {
        for (int i = 0; i < bullets.size(); i++) {
            Bullet bullet = bullets.get(i);
            if (boss.contains(bullet.x, bullet.y)) {
                playSound(explosionSound);
                boss.health -= 1;
                bullets.remove(i);
                i--;
            }
        }
        for (int i = 0; i < enemyBullets.size(); i++) {
            Bullet bullet = enemyBullets.get(i);
            if (player.contains(bullet.x, bullet.y)) {
                playSound(explosionSound);
                player.health -= 5;
                enemyBullets.remove(i);
                i--;
            }
        }

        if (boss.contains(player.x, player.y)) {
            player.health -= 10;
            playSound(explosionSound);
        }

        checkGameOver();
    }

    private void drawHealthBars(Graphics2D g2d) {
        g2d.setColor(Color.GREEN);
        g2d.fillRect(10, 10, player.health, 20);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(10, 10, 100, 20);
        g2d.drawString("Player", 20, 25);
        g2d.setColor(Color.RED);
        g2d.fillRect(getWidth() - boss.health * 2 - 30, 10, boss.health * 2, 20);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(570, 10, 200, 20);
        g2d.drawString("Boss", getWidth() - 70, 25);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        player.keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        player.keyReleased(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mousePosition = e.getPoint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!gameOver) {
            bullets.add(player.shoot(mousePosition));
            playSound(shootSound);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    class PlayerShip {
        private int x, y;
        private int vx, vy;
        public int health = 100;

        public PlayerShip(int x, int y) {
            this.x = x;
            this.y = y;
            this.vx = 0;
            this.vy = 0;
        }

        public void update() {
            x = Math.min(Math.max(x + vx, 0), getWidth() - 30);
            y = Math.min(Math.max(y + vy, 0), getHeight() - 40);
        }

        public void draw(Graphics2D g2d, Point mouse) {
            double angle = Math.atan2(mouse.y - y, mouse.x - x) + Math.PI / 2;
            AffineTransform originalTransform = g2d.getTransform();
            g2d.translate(x, y);
            g2d.rotate(angle);

            g2d.drawImage(new ImageIcon("src/images/ship.png").getImage(), -15, -20, 30, 40, null);

            g2d.setTransform(originalTransform);
        }

        public void keyPressed(KeyEvent e) {
            int SPEED = 5;
            if (e.getKeyCode() == KeyEvent.VK_W) vy = -SPEED;
            if (e.getKeyCode() == KeyEvent.VK_S) vy = SPEED;
            if (e.getKeyCode() == KeyEvent.VK_A) vx = -SPEED;
            if (e.getKeyCode() == KeyEvent.VK_D) vx = SPEED;
        }

        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_S) vy = 0;
            if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_D) vx = 0;
        }

        public boolean contains(int bulletX, int bulletY) {
            return new Rectangle(x - 15, y - 10, 30, 20).contains(bulletX, bulletY);
        }

        public Bullet shoot(Point target) {
            return new Bullet(x, y, target.x, target.y);
        }
    }

    class EnemyMandala {
        public int x, y;
        public int health = 100;
        private int dx, dy;

        public EnemyMandala(int x, int y) {
            this.x = x;
            this.y = y;
            randomizeDirection();
        }

        public void update() {
            x += dx;
            y += dy;

            if (x < 50 || x > 750) dx = -dx;
            if (y < 50 || y > 550) dy = -dy;

            if (random.nextDouble() < 0.02) randomizeDirection();

            if (random.nextDouble() < 0.01) {
                enemyBullets.add(new Bullet(x, y, player.x, player.y));
                playSound(shootSound);
            }
        }

        private void randomizeDirection() {
            int SPEED = 7;
            dx = random.nextInt(2 * SPEED + 1) - SPEED;
            dy = random.nextInt(2 * SPEED + 1) - SPEED;
        }

        public boolean contains(int x, int y) {
            return new Ellipse2D.Double(this.x - 50, this.y - 50, 100, 100).contains(x, y);
        }
    }


    static class Bullet {
        public int x, y;
        private final int dx, dy;

        public Bullet(int startX, int startY, int targetX, int targetY) {
            x = startX;
            y = startY;
            double angle = Math.atan2(targetY - startY, targetX - startX);
            int SPEED = 10;
            dx = (int) (SPEED * Math.cos(angle));
            dy = (int) (SPEED * Math.sin(angle));
        }

        public void update() {
            x += dx;
            y += dy;
        }

        public boolean isOffScreen(int width, int height) {
            return x < 0 || x > width || y < 0 || y > height;
        }
    }

    public static void main(String[] args) {
        boolean start = JOptionPane.showConfirmDialog(null, """
                        Bienvenido a GameChuy
                        
                        Instrucciones:
                        1. Mueve la nave con las teclas W, A, S, D
                        2. Dispara con el clic izquierdo del mouse\s
                        apuntando al centro del jefe
                        3. Evita los disparos del jefe
                        4. No dejes que el centro del jefe te toque
                        5. Elimina al jefe antes de que te elimine
                        
                        Presiona OK para comenzar
                        
                        ¡Buena suerte!
                        
                        ¿Deseas comenzar?""",
                "GameChuy", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;

        if (start) {
            SwingUtilities.invokeLater(() -> new GameChuy().setVisible(true));
        } else {
            JOptionPane.showMessageDialog(null, "¡Hasta luego!");
        }
    }
}
