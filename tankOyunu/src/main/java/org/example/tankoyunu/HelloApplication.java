package org.example.tankoyunu;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HelloApplication extends Application {

    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;
    private static final int TANK_SIZE = 50;
    private static final int PLAYER_TANK_SPEED = 8;
    private static final int ENEMY_TANK_SPEED = 3;
    private static final int BULLET_SIZE = 10;
    private static final double BULLET_SPEED = 5.0;
    private static final int ENEMY_BULLET_SIZE = 10;
    private static final double ENEMY_BULLET_SPEED = 1.5;
    private static final int MAX_PLAYER_LIVES = 1;

    private int tankX = SCREEN_WIDTH / 2 - TANK_SIZE / 2;
    private int tankY = SCREEN_HEIGHT / 2 - TANK_SIZE / 2;
    private boolean isFiring = false;
    private int tankDirection = 1;

    private List<Bullet> bullets = new ArrayList<>();
    private List<EnemyTank> enemyTanks = new ArrayList<>();
    private List<EnemyBullet> enemyBullets = new ArrayList<>();

    private boolean gameOver = false;
    private int score = 0;

    private static final long PLAYER_BULLET_COOLDOWN = 500_000_000;
    private long lastPlayerBulletTime = 0;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Tank Game");

        Canvas canvas = new Canvas(SCREEN_WIDTH, SCREEN_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        StackPane root = new StackPane();
        root.getChildren().add(canvas);

        Scene scene = new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT);

        for (int i = 0; i < 3; i++) {
            EnemyTank enemyTank = new EnemyTank();
            enemyTanks.add(enemyTank);
        }

        scene.setOnKeyPressed(e -> {
            if (!gameOver) {
                if (e.getCode() == KeyCode.LEFT && tankX > 0) {
                    tankX -= PLAYER_TANK_SPEED;
                    tankDirection = 2;
                } else if (e.getCode() == KeyCode.RIGHT && tankX < SCREEN_WIDTH - TANK_SIZE) {
                    tankX += PLAYER_TANK_SPEED;
                    tankDirection = 1;
                } else if (e.getCode() == KeyCode.UP && tankY > 0) {
                    tankY -= PLAYER_TANK_SPEED;
                    tankDirection = 3;
                } else if (e.getCode() == KeyCode.DOWN && tankY < SCREEN_HEIGHT - TANK_SIZE) {
                    tankY += PLAYER_TANK_SPEED;
                    tankDirection = 0;
                } else if (e.getCode() == KeyCode.SPACE) {
                    isFiring = true;
                }
            }
        });

        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.SPACE) {
                isFiring = false;
            }
        });

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!gameOver) {
                    updateBullets();
                    updateEnemyTanks();
                    updateEnemyBullets();
                    checkCollisions();
                }
                draw(gc);
            }
        }.start();

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void draw(GraphicsContext gc) {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        if (!gameOver) {
            Image tankImage = new Image("oyuncu.png");

            double rotationAngle = 0;
            switch (tankDirection) {
                case 0:
                    rotationAngle = 90;
                    break;
                case 1:
                    rotationAngle = 0;
                    break;
                case 2:
                    rotationAngle = 180;
                    break;
                case 3:
                    rotationAngle = 270;
                    break;
                default:
                    rotationAngle = 180;
                    break;
            }

            gc.save();
            gc.translate(tankX + TANK_SIZE / 2, tankY + TANK_SIZE / 2);
            gc.rotate(rotationAngle);
            gc.drawImage(tankImage, -TANK_SIZE / 2, -TANK_SIZE / 2, TANK_SIZE, TANK_SIZE);
            gc.restore();

            // Oyuncu mermilerini çiz
            gc.setFill(Color.RED);
            for (Bullet bullet : bullets) {
                gc.fillRect(bullet.getX(), bullet.getY(), BULLET_SIZE, BULLET_SIZE);
            }

            // Düşman tanklarını çiz
            gc.setFill(Color.GREEN);
            for (EnemyTank enemyTank : enemyTanks) {
                if (!enemyTank.isDestroyed()) {
                    Image tankImagee = enemyTank.getTankImage();
                    gc.drawImage(tankImagee, enemyTank.getX(), enemyTank.getY(), TANK_SIZE, TANK_SIZE);
                }
            }

            // Düşman mermilerini çiz
            gc.setFill(Color.BLUE);
            for (EnemyBullet enemyBullet : enemyBullets) {
                gc.fillRect(enemyBullet.getX(), enemyBullet.getY(), ENEMY_BULLET_SIZE, ENEMY_BULLET_SIZE);
            }

            // Oyuncu puanını ekrana yazdır
            gc.setFill(Color.WHITE);
            gc.setFont(new Font("Arial", 20));
            gc.fillText("Puan: " + score, 10, 20);
        } else {
            // Oyun bittiyse sadece puanı göster
            gc.setFill(Color.WHITE);
            gc.setFont(new Font("Arial", 36));
            gc.fillText("Toplam Puan: " + score, SCREEN_WIDTH / 2 - 180, SCREEN_HEIGHT / 2 - 30);
        }
    }

    private void updateBullets() {
        long currentTime = System.nanoTime();

        if (isFiring && currentTime - lastPlayerBulletTime > PLAYER_BULLET_COOLDOWN) {
            Bullet bullet = new Bullet(
                    tankX + TANK_SIZE / 2 - BULLET_SIZE / 2,
                    tankY + TANK_SIZE / 2 - BULLET_SIZE / 2,
                    BULLET_SPEED,
                    tankDirection
            );

            bullets.add(bullet);

            lastPlayerBulletTime = currentTime;
        }

        List<Bullet> bulletsToRemove = new ArrayList<>();
        for (Bullet bullet : bullets) {
            bullet.update();

            // Düşman tanklarıyla çarpışma kontrolü
            for (EnemyTank enemyTank : enemyTanks) {
                if (bullet.collidesWith(enemyTank)) {
                    bulletsToRemove.add(bullet);     // Çarpışma durumunda mermiyi kaldır
                    enemyTank.destroy();    // Düşman tankını yok et
                    score += 10;
                }
            }
            // Mermi ekran dışına çıkarsa, kaldırılacak mermi listesine ekle
            if (isBulletOutsideScreen(bullet)) {
                bulletsToRemove.add(bullet);
            }
        }
        // Kaldırılacak mermileri ana listeden kaldır
        bullets.removeAll(bulletsToRemove);
    }

    private boolean isBulletOutsideScreen(Bullet bullet) {
        if (bullet.getDirection() == 1) {
            return bullet.getX() > SCREEN_WIDTH;
        } else if (bullet.getDirection() == 2) {
            return bullet.getX() + BULLET_SIZE < 0;
        } else {
            return false;
        }
    }

    private void updateEnemyTanks() {
        for (EnemyTank enemyTank : enemyTanks) {
            enemyTank.update();
            enemyTank.fixBounds();

            if (!enemyTank.isDestroyed() && Math.random() < 0.03) {
                // Düşman tankından mermi ateşle
                enemyTank.fireBullet();
            }
        }

        boolean allTanksDestroyed = true;
        for (EnemyTank enemyTank : enemyTanks) {
            if (!enemyTank.isDestroyed()) {
                allTanksDestroyed = false;
                break;
            }
        }

        if (allTanksDestroyed) {
            gameOver = true;
        }
    }

    private void updateEnemyBullets() {
        List<EnemyBullet> bulletsToRemove = new ArrayList<>();
        List<EnemyTank> tanksToRemove = new ArrayList<>();

        for (EnemyBullet bullet : enemyBullets) {
            bullet.update();

            for (EnemyTank enemyTank : enemyTanks) {
                if (enemyTank.isDestroyed() && bullet.collidesWith(enemyTank.getX(), enemyTank.getY(), TANK_SIZE, TANK_SIZE)) {
                    tanksToRemove.add(enemyTank);
                    bulletsToRemove.add(bullet);
                }
            }

            // Eğer mermi ekran dışına çıkarsa, mermiyi kaldırılacak listeye ekle
            if (bullet.getX() < 0 || bullet.getX() > SCREEN_WIDTH || bullet.getY() < 0 || bullet.getY() > SCREEN_HEIGHT) {
                bulletsToRemove.add(bullet);
            }
        }

        enemyBullets.removeAll(bulletsToRemove);

        // Kaldırılacak tankları yok et ve tekrar oluştur
        for (EnemyTank removedTank : tanksToRemove) {
            removedTank.destroy();
            removedTank.respawn();
        }
    }

    private void checkCollisions() {

        // Oyuncu tankı ile düşman tankları arasındaki çarpışmayı kontrol et
        for (EnemyTank enemyTank : enemyTanks) {
            if (!enemyTank.isDestroyed() &&
                    tankX < enemyTank.getX() + TANK_SIZE &&
                    tankX + TANK_SIZE > enemyTank.getX() &&
                    tankY < enemyTank.getY() + TANK_SIZE &&
                    tankY + TANK_SIZE > enemyTank.getY()) {
                gameOver = true;
            }
        }

        // Oyuncu tankı ile düşman mermileri arasındaki çarpışmayı kontrol et
        List<EnemyBullet> bulletsToRemove = new ArrayList<>();
        for (EnemyBullet enemyBullet : enemyBullets) {
            if (enemyBullet.collidesWith(tankX, tankY, TANK_SIZE, TANK_SIZE)) {
                gameOver = true;
                bulletsToRemove.add(enemyBullet);
            }
        }

        enemyBullets.removeAll(bulletsToRemove);
    }

    private static class Bullet {
        private double x;
        private double y;
        private double speed;
        private int direction;

        public Bullet(double x, double y, double speed, int direction) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.direction = direction;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public void update() {
            switch (direction) {
                case 0:
                    y += speed; // aşağı
                    break;
                case 1:
                    x += speed; // sağ
                    break;
                case 2:
                    x -= speed; // sol
                    break;
                case 3:
                    y -= speed; // yukarı
                    break;
            }
        }

        public boolean collidesWith(EnemyTank enemyTank) {

            // Mermi ve düşman tankı arasındaki çarpışmayı kontrol et
            return x < enemyTank.getX() + TANK_SIZE &&
                    x + BULLET_SIZE > enemyTank.getX() &&
                    y < enemyTank.getY() + TANK_SIZE &&
                    y + BULLET_SIZE > enemyTank.getY();
        }

        private int getDirection() {
            return direction;
        }
    }

    private static class EnemyBullet {
        private double x;
        private double y;
        private double speedX;
        private double speedY;
        private double speed;

        public EnemyBullet(double x, double y, double speedX, double speedY, double speed) {
            this.x = x;
            this.y = y;
            this.speedX = speedX;
            this.speedY = speedY;
            this.speed = speed;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public void update() {
            x += speedX * speed;
            y += speedY * speed;
        }

        public boolean collidesWith(double otherX, double otherY, double otherWidth, double otherHeight) {

            // Mermi ve diğer nesne arasındaki çarpışmayı kontrol et
            return x < otherX + otherWidth &&
                    x + ENEMY_BULLET_SIZE > otherX &&
                    y < otherY + otherHeight &&
                    y + ENEMY_BULLET_SIZE > otherY;
        }
    }

    public class EnemyTank {
        private static final long ENEMY_DIRECTION_CHANGE_COOLDOWN = 1_000_000_000;
        private static final long RESPAWN_COOLDOWN = 1_000_000_000;

        private double x;
        private double y;
        private double speedX;
        private double speedY;
        private boolean destroyed;
        private Image tankImage;

        private long lastDirectionChangeTime = 0;
        private long respawnCooldown = 0;

        public EnemyTank() {
            setRandomPosition();
            setRandomDirection();
            tankImage = new Image("düşman.png");
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public boolean isDestroyed() {
            return destroyed;
        }

        public void respawn() {
            destroyed = false;
            setRandomPosition();
            respawnCooldown = System.nanoTime() + RESPAWN_COOLDOWN;
        }



        public void destroy() {
            destroyed = true;
            x = -1;
            y = -1;
        }

        public void update() {
            if (destroyed) {
                if (System.nanoTime() > respawnCooldown) {
                    respawn();
                }
            } else {
                long currentTime = System.nanoTime();

                if (currentTime - lastDirectionChangeTime > ENEMY_DIRECTION_CHANGE_COOLDOWN) {
                    setRandomDirection();
                    lastDirectionChangeTime = currentTime;
                }

                x += speedX;
                y += speedY;
            }
        }

        public Image getTankImage() {
            double rotationAngle = 0;
            switch ((int) Math.toDegrees(Math.atan2(speedY, speedX))) {
                case 0:
                    rotationAngle = 0; // sağ
                    break;
                case 90:
                    rotationAngle = 90; // aşağı
                    break;
                case 180:
                    rotationAngle = 180; // sol
                    break;
                case -90:
                    rotationAngle = 270; // yukarı
                    break;
                default:
                    rotationAngle = 0;
                    break;
            }


            return rotateImage(new Image("düşman.png"), rotationAngle);
        }

        private Image rotateImage(Image image, double angle) {
            ImageView imageView = new ImageView(image);
            imageView.setRotate(angle);

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            return imageView.snapshot(params, null);
        }


        public void fixBounds() {
            if (x < 0) {
                x = 0;
                setRandomDirection();
            } else if (x > SCREEN_WIDTH - TANK_SIZE) {
                x = SCREEN_WIDTH - TANK_SIZE;
                setRandomDirection();
            }

            if (y < 0) {
                y = 0;
                setRandomDirection();
            } else if (y > SCREEN_HEIGHT - TANK_SIZE) {
                y = SCREEN_HEIGHT - TANK_SIZE;
                setRandomDirection();
            }
        }

        public void fireBullet() {
            enemyBullets.add(new EnemyBullet(
                    x + TANK_SIZE / 2 - ENEMY_BULLET_SIZE / 2,
                    y + TANK_SIZE / 2 - ENEMY_BULLET_SIZE / 2,
                    speedX,
                    speedY,
                    ENEMY_BULLET_SPEED
            ));
        }

        private void setRandomPosition() {
            Random random = new Random();
            x = random.nextInt(SCREEN_WIDTH - TANK_SIZE + 1);
            y = random.nextInt(SCREEN_HEIGHT - TANK_SIZE + 1);
        }

        private void setRandomDirection() {
            Random random = new Random();
            int direction = random.nextInt(4); // 0: aşağı , 1: sağ, 2: sol, 3: yukarı

            switch (direction) {
                case 0:
                    speedX = 0;
                    speedY = ENEMY_TANK_SPEED;
                    break;
                case 1:
                    speedX = ENEMY_TANK_SPEED;
                    speedY = 0;
                    break;
                case 2:
                    speedX = -ENEMY_TANK_SPEED;
                    speedY = 0;
                    break;
                case 3:
                    speedX = 0;
                    speedY = -ENEMY_TANK_SPEED;
                    break;
            }
        }
    }
}