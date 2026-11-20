import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import javax.sound.sampled.*;
import javax.swing.*;

public class CoinWorld extends JPanel {
    private static final int TILE = 48;
    private static final int SPEED = 4;
    private static final int POINTS_PER_COIN = 10;

    private long score = 0;
    private long playerX = TILE / 2;
    private long playerY = TILE / 2;
    private int frame = 0;
    private Clip music;

    private record Cell(long x, long y) {}

    private final Set<Cell> collected = new HashSet<>();
    private final Set<Integer> keys = new HashSet<>();
    private final BufferedImage[] grass = new BufferedImage[4];

    // Each digit is a color. A dot is a transparent pixel.
    private final BufferedImage coin = sprite(new String[] {
        "..1111..",
        ".122221.",
        "12233221",
        "12323221",
        "12323221",
        "12233221",
        ".122221.",
        "..1111.."
    }, 0x925118, 0xFAB727, 0xFFF48D);

    private final BufferedImage player = sprite(new String[] {
        "..1111..",
        ".111111.",
        "..2222..",
        "..2323..",
        ".444444.",
        "22444422",
        "..5555..",
        ".55..55."
    }, 0x783E25, 0xFFC589, 0x2D2223, 0x45AAEA, 0x24375B);

    public CoinWorld() {
        setPreferredSize(new Dimension(800, 540));
        setFocusable(true);

        for (int i = 0; i < grass.length; i++) {
            grass[i] = makeGrass(i);
        }

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                keys.add(event.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent event) {
                keys.remove(event.getKeyCode());
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                keys.clear();
            }
        });
    }

    private void playMusic() {
        File file = new File("track.wav");

        try (AudioInputStream audio = AudioSystem.getAudioInputStream(file)) {
            music = AudioSystem.getClip();
            music.open(audio);
            music.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception error) {
            if (music != null) {
                music.close();
            }
            System.err.println("Could not play " + file.getAbsolutePath());
            System.err.println(error.getMessage());
        }
    }

    private void updateGame() {
        int dx = 0;
        int dy = 0;

        if (keys.contains(KeyEvent.VK_A) || keys.contains(KeyEvent.VK_LEFT)) {
            dx--;
        }
        if (keys.contains(KeyEvent.VK_D) || keys.contains(KeyEvent.VK_RIGHT)) {
            dx++;
        }
        if (keys.contains(KeyEvent.VK_W) || keys.contains(KeyEvent.VK_UP)) {
            dy--;
        }
        if (keys.contains(KeyEvent.VK_S) || keys.contains(KeyEvent.VK_DOWN)) {
            dy++;
        }

        int step = SPEED;
        if (dx != 0 && dy != 0) {
            step = (int) Math.round(SPEED / Math.sqrt(2));
        }

        playerX += dx * step;
        playerY += dy * step;

        long tileX = Math.floorDiv(playerX, TILE);
        long tileY = Math.floorDiv(playerY, TILE);

        int offsetX = Math.floorMod(playerX, TILE) - TILE / 2;
        int offsetY = Math.floorMod(playerY, TILE) - TILE / 2;
        boolean touchingCoin = offsetX * offsetX + offsetY * offsetY <= 18 * 18;

        if (touchingCoin && hasCoin(tileX, tileY)) {
            if (collected.add(new Cell(tileX, tileY))) {
                score += POINTS_PER_COIN;
            }
        }

        frame = (frame + 1) % 60;
        repaint();
    }

    private static long tileHash(long x, long y) {
        long hash = x * 73856093L ^ y * 19349663L;
        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        return hash ^ (hash >>> 33);
    }

    private static boolean hasCoin(long x, long y) {
        if (x == 0 && y == 0) return false;
        if (x == 1 && y == 0) return true; // First coin is to the right.

        return Math.floorMod(tileHash(x, y), 8) == 0;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();

        g.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );

        long cameraX = playerX - getWidth() / 2;
        long cameraY = playerY - getHeight() / 2;

        long firstX = Math.floorDiv(cameraX, TILE);
        long firstY = Math.floorDiv(cameraY, TILE);

        int startX = -Math.floorMod(cameraX, TILE);
        int startY = -Math.floorMod(cameraY, TILE);
        int bob = (int) Math.round(2 * Math.sin(frame * Math.PI / 30));

        // Draw only the tiles visible in the window.
        for (int row = 0; startY + row * TILE < getHeight(); row++) {
            for (int col = 0; startX + col * TILE < getWidth(); col++) {
                long x = firstX + col;
                long y = firstY + row;

                int screenX = startX + col * TILE;
                int screenY = startY + row * TILE;

                int variant = Math.floorMod(tileHash(x, y), grass.length);
                g.drawImage(grass[variant], screenX, screenY, TILE, TILE, null);

                if (hasCoin(x, y) && !collected.contains(new Cell(x, y))) {
                    g.drawImage(coin, screenX + 12, screenY + 12 + bob, 24, 24, null);
                }
            }
        }

        g.drawImage(player, getWidth() / 2 - 16, getHeight() / 2 - 16, 32, 32, null);

        g.setColor(new Color(20, 32, 27, 225));
        g.fillRoundRect(12, 12, 240, 64, 12, 12);

        g.setColor(new Color(255, 221, 98));
        g.setFont(new Font("Monospaced", Font.BOLD, 19));
        g.drawString("Score: " + score, 24, 38);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g.drawString("Move: WASD or arrow keys", 24, 61);

        g.dispose();
    }

    private static BufferedImage makeGrass(int seed) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Random random = new Random(seed);

        g.setColor(new Color(69, 133, 74));
        g.fillRect(0, 0, 16, 16);

        for (int i = 0; i < 18; i++) {
            Color blade = i % 2 == 0 ? new Color(86, 153, 77) : new Color(52, 113, 65);
            g.setColor(blade);
            g.fillRect(random.nextInt(16), random.nextInt(16), 1, 2);
        }

        g.dispose();
        return image;
    }

    private static BufferedImage sprite(String[] rows, int... colors) {
        BufferedImage image = new BufferedImage(
            rows[0].length(), rows.length, BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                char pixel = rows[y].charAt(x);
                if (pixel != '.') {
                    image.setRGB(x, y, 0xff000000 | colors[pixel - '1']);
                }
            }
        }

        return image;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Coin World");
            CoinWorld game = new CoinWorld();

            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(game);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);

            game.requestFocusInWindow();
            game.playMusic();

            new Timer(16, event -> game.updateGame()).start();
        });
    }
}