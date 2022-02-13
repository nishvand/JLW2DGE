package nishvand;

import nishvand.Misc.InfiniteLoopThread;
import nishvand.Objects.Block;
import nishvand.Objects.Button;
import nishvand.Objects.Light;
import nishvand.Objects.Sprite;
import nishvand.World.Structure;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static nishvand.Objects.Sprite.upscale;

public class Engine extends Canvas implements Runnable {
    protected int X = 0;
    protected int Y = 0;
    ArrayList<Sprite> sprites = new ArrayList<>();
    ArrayList<Sprite> bottomLayer = new ArrayList<>();
    ArrayList<Light> lightSources = new ArrayList<>();
    private boolean[] keys = { false, false, false, false }; // нажатые клавиши
    public boolean ctrl = false; // и это тоже
    public boolean shift = false;
    public boolean mouseButton = false;
    public boolean mouseInWindow = true, drawLine = false;
    private boolean showForeground = false;
    private boolean takeControl = false;
    public int mouseX = 0, mouseY = 0; // координаты мыши относительно окна
    public int fps = 1, frames = 0; // фпс и фреймы
    protected BufferedImage foreground;
    public String material = "404";
    Queue<Integer> TouchIDs = new ArrayDeque<>();

    void launch(){
        new Thread(this).start(); // запускаем движок
    }

    public void updateTextures() { // 20 раз в секунду обновляем текстуры анимированных объектов
        for (Sprite sprite : sprites) {
            sprite.updateTexture();
        }
    }

    private InfiniteLoopThread getRenderThread(){
        return new InfiniteLoopThread() {
            @Override
            public void task() throws Exception {
                try {
                    render();
                    frames++;
                }
                catch (ConcurrentModificationException | IndexOutOfBoundsException ignored) {}
            }
        };
    }

    @Override
    public void run() { // при запуске
        init(); // инициализируем
        InfiniteLoopThread renderThread = getRenderThread();
        renderThread.start();
    }

    public String debugInfo() {
        return "FPS: " + fps + " OBJECTS:" + sprites.size() + " LIGHTS:" + lightSources.size();
    }

    public void init() {
        BufferStrategy bs = getBufferStrategy();
        createBufferStrategy(2); // создаем BufferStrategy для нашего холста
        requestFocus();

        try {
            f = Font.createFont(Font.TRUETYPE_FONT, findFile("font.ttf")).deriveFont((float) 32);
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadingScreen(); // показываем экран загрузки

        addKeyListener(getKeyboardListener()); // устанавливаем слушатель нажатий на клавиши
        addMouseMotionListener(getMotionListener()); // и так же слушатель изменения позиции мыши
        addMouseListener(getMouseListener()); // и еще слушатель кликов на мыши

        mouseX = (Main.width / 2);
        mouseY = (Main.height/ 2);
        initThreads();

        getMenu();
    }

    public void getMenu(){
        sprites.clear();
        lightSources.clear();
        bottomLayer.clear();
        showForeground = false;
        takeControl = true;
        drawLine = false;

        Button start = new Button(upscale(stringToIMG("Play!", new Color(255, 255, 255), 64), 4), 100, (Main.height / 2)) {
            @Override
            public void onClick() {
                System.out.println();
                loadingScreen();
                getMap();
            }
        };

        sprites.add(new Block(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)));
        sprites.add(new Block(upscale(stringToIMG("Engine playground!"), 2)));
        sprites.add(start);
    }

    public void getMap() {
        sprites.clear();
        lightSources.clear();
        bottomLayer.clear();
        showForeground = true;
        setZero();

        // добавляем персонажа
        sprites.add(createSprite(findImage("observer_front")));
        sprites.get(0).upscale(4);

        // добавляем пол
        System.out.println("creating floor");
        Sprite floor = createSprite(findImage("grass_top"));
        floor.upscale(4);
        floor.createMatrix(128, true);
        floor.addAttribute("floor");
        floor.center();
        bottomLayer.add(floor);

        ArrayList<Sprite> start = new Structure("start").compile();
        bottomLayer.add(start.get(0));
        for (int i = 1; i < start.size(); i++) {
            sprites.add(start.get(i));
        }
        drawLine = true;
        takeControl = false;
    }

    public void setZero() {
        X = (Main.width / 2);
        Y = (Main.height / 2);
    }

    // простые методы для создания обычного спрайта
    static public Sprite createSprite(Image img, int x, int y, boolean collision){ return new Block(img, x, y, collision); }
    static public Sprite createSprite(Image img, int x, int y){ return new Block(img, x, y); }
    static public Sprite createSprite(Image img){ return new Block(img); }

    // поиск изображения в ресурсах jar файла
    static public Image findImage(String name) {
        try { // пытаемся открыть png файл
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name + (name.contains(".png") ? "" : ".png"));
            assert is != null;
            return ImageIO.read(is); // и возвращаем готовое изображение
        } catch (IOException ex){ return findImage("404"); } // если не нашли, заменяем на 404 текстуру
    }

    static public InputStream findFile(String name){
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }

    public void loadingScreen() {
        sprites.clear();
        lightSources.clear();
        bottomLayer.clear();

        BufferStrategy bs = getBufferStrategy();
        Graphics g = bs.getDrawGraphics(); // получаем Graphics из BufferStrategy
        g.setColor(Color.black); // цвет фона
        g.fillRect(0, 0, getWidth(), getHeight()); // заполняем

        Sprite loading = createSprite(findImage("loading")); // просто рисуем спрайт загрузки
        loading.upscale(4);
        loading.draw(g, 0, 0);

        g.dispose();
        bs.show(); // показываем кадр
    }

    public void render() {
        BufferStrategy bs = getBufferStrategy();
        Graphics2D g = (Graphics2D)getBufferStrategy().getDrawGraphics(); // получаем Graphics из BufferStrategy

        g.setColor(Color.black); // цвет фона
        g.fillRect(0, 0, getWidth(), getHeight()); // заполняем

        for (Sprite temp : bottomLayer) { // рендерим все спрайты на нижнем слое на их позициях
            temp.draw(g, X + temp.offsetX, Y + temp.offsetY);
        }

        for(int i = 1; i < sprites.size(); i++) { // рендерим все тени на их позициях
            Sprite temp = sprites.get(i);
            temp.drawShadow(g, X + temp.offsetX, Y + temp.offsetY);
        }

        for(int i = 1; i < sprites.size(); i++) { // рендерим все спрайты на их позициях
            Sprite temp = sprites.get(i);
            temp.draw(g, X + temp.offsetX, Y + temp.offsetY);
        }

        if(drawLine && mouseInWindow) { // если мышка находится в зоне окна
            g.setColor(new Color(255, 255, 255, 63)); // рисуем линию к ней
            g.setStroke(new BasicStroke(5));
            g.drawLine((Main.width / 2), (Main.height / 2), mouseX, mouseY);
        }

        Sprite h = sprites.get(0); // после этого рендерим персонажа
        h.draw(g, (Main.width / 2) - (h.getImage().getWidth(null) / 2), (Main.height / 2) - (h.getImage().getHeight(null) / 2));

        for (Light temp : lightSources) { // рендерим все исочники света
            temp.draw(g, X + temp.offsetX, Y + temp.offsetY);
        }

        if(showForeground) g.drawImage(foreground, 0, 0, null);

        g.dispose();
        bs.show(); // показываем кадр
    }

    Font f = null;
    public BufferedImage stringToIMG(String s) { return stringToIMG(s, Color.WHITE, 32); }
    public BufferedImage stringToIMG(String s, Color c, int size) {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        g.setFont(f);

        FontRenderContext frc = g.getFontMetrics().getFontRenderContext();
        Rectangle2D rect = f.getStringBounds(s, frc);
        g.dispose();

        img = new BufferedImage((int) Math.ceil(rect.getWidth()), (int) Math.ceil(rect.getHeight()), BufferedImage.TYPE_4BYTE_ABGR);
        g = img.getGraphics();
        g.setColor(c);
        g.setFont(f);

        FontMetrics fm = g.getFontMetrics();
        int x = 0;
        int y = fm.getAscent();
        g.drawString(s, x, y);

        g.dispose();
        return img;
    }

    public void keyPress(int key){ //w0 a1 s2 d3
        keys[key] = true;
    } // если нажата клавиша
    public void keyRelease(int key){
        keys[key] = false;
    } // если отпущена

    public void updatePos() { // 120 раз в секунду обновляем позицию игрока
        if(takeControl) return;
        int speed = 2;
        if(ctrl) speed /= 2;
        if(shift) speed *= 2;

        if (keys[3]) { //a
            // проверяем на какое расстояние мы можем пройти если почти уперлись в стену
            int temp = speed; for(; !checkCollision( temp, 0); temp--);
            X -= temp;
        } if (keys[1]) { //d
            int temp = speed; for(; !checkCollision(-temp, 0); temp--);
            X += temp;
        } if (keys[0]) { //w
            int temp = speed; for(; !checkCollision(0, -temp); temp--);
            Y += temp;
        } if (keys[2]) { //s
            int temp = speed; for(; !checkCollision(0,  temp); temp--);
            Y -= temp;
        }
    }

    // проверяем можем ли мы пройти
    public boolean checkCollision(int nextX, int nextY) {
        boolean collision = true;
        for(int i = 1; i < sprites.size(); i++){
            if(collide(i, nextX, nextY)) { // проверяем упираемся ли мы в спрайт
                if (sprites.get(i).collision) // если у него есть коллизия
                    collision = false; // если да то мы упираемся
                TouchIDs.add(i); // и уведомим объект о соприкосновении
            }
        }

        return collision; // если путь свободен
    }

    // проверка на нахождение игрока в объекте
    public boolean collide(int ID, int nextX, int nextY) { return collide(0, ID, nextX, nextY); }
    public boolean collide(int ID, int ID2, int nextX, int nextY) {
        Sprite ABCD = sprites.get(ID2); // получаем объект
        Sprite A1B1C1D1 = sprites.get(ID); // и игрока
        int x = (Main.width / 2) - (sprites.get(0).getImage().getWidth(null) / 2) + nextX;
        int y = (Main.height / 2)- (sprites.get(0).getImage().getHeight(null)/ 2) + nextY;

        // вычисляем нужные нам точки
        int BX = X + ABCD.offsetX;
        int BY = Y + ABCD.offsetY;
        int DX = X + ABCD.offsetX + ABCD.getWidth();
        int DY = Y + ABCD.offsetY + ABCD.getHeight();

        int A1X = x + A1B1C1D1.offsetX;
        int A1Y = y + A1B1C1D1.offsetY + A1B1C1D1.getHeight();
        int B1X = x + A1B1C1D1.offsetX;
        int B1Y = y + A1B1C1D1.offsetY;
        int C1X = x + A1B1C1D1.offsetX + A1B1C1D1.getWidth();
        int C1Y = y + A1B1C1D1.offsetY;
        int D1X = x + A1B1C1D1.offsetX + A1B1C1D1.getWidth();
        int D1Y = y + A1B1C1D1.offsetY + A1B1C1D1.getHeight();

        // проверяем если находится точка игрока внутри объекта
        if(B1X >= BX && B1Y >= BY && DX >= B1X && DY >= B1Y) return true;
        if(D1X >= BX && D1Y >= BY && DX >= D1X && DY >= D1Y) return true;
        if(C1X >= BX && C1Y >= BY && DX >= C1X && DY >= C1Y) return true;
        return A1X >= BX && A1Y >= BY && DX >= A1X && DY >= A1Y;
    }

    public boolean dotCollide(int x, int y, int ID){
        Sprite ABCD = sprites.get(ID); // получаем объект
        // вычисляем его точки
        int BX = X + ABCD.offsetX;
        int BY = Y + ABCD.offsetY;
        int DX = X + ABCD.offsetX + ABCD.getWidth();
        int DY = Y + ABCD.offsetY + ABCD.getHeight();

        // проверяем если находится точка игрока внутри объекта
        return x >= BX && y >= BY && DX >= x && DY >= y;
    }

    // подсчет дистанции до объекта
    public int distance(int x, int y, int x1, int y1) {
        int AC = x1 - x;
        int BC = y1 - y;
        return (int) Math.sqrt(AC*AC + BC*BC);
    }

    public void onResize(int x, int y) {
        X = X - (Main.width / 2)  + (x / 2);
        Y = Y - (Main.height / 2) + (y / 2);
    }

    public void onMouseClick() {
        for (int i = 1; i < sprites.size(); i++){
            if(dotCollide(mouseX, mouseY, i) && !sprites.get(i).hasAttribute("floor")) {
                sprites.get(i).onHit();
            }
        }
    }


    private KeyListener getKeyboardListener(){
        return new KeyListener(){
            @Override public void keyTyped(KeyEvent keyEvent) {}

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if(keyEvent.getKeyCode() == KeyEvent.VK_W){ keyPress(0); }
                if(keyEvent.getKeyCode() == KeyEvent.VK_A){ keyPress(1); }
                if(keyEvent.getKeyCode() == KeyEvent.VK_S){ keyPress(2); }
                if(keyEvent.getKeyCode() == KeyEvent.VK_D){ keyPress(3); }
                if(keyEvent.getKeyCode() == KeyEvent.VK_CONTROL) { ctrl = true; }
                if(keyEvent.getKeyCode() == KeyEvent.VK_SHIFT) { shift = true; }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
                if(keyEvent.getKeyCode() == KeyEvent.VK_W){ keyRelease(0); }
                if(keyEvent.getKeyCode() == KeyEvent.VK_A){ keyRelease(1); }
                if(keyEvent.getKeyCode() == KeyEvent.VK_S){ keyRelease(2); }
                if(keyEvent.getKeyCode() == KeyEvent.VK_D){ keyRelease(3); }
                if(keyEvent.getKeyCode() == KeyEvent.VK_CONTROL) { ctrl = false; }
                if(keyEvent.getKeyCode() == KeyEvent.VK_SHIFT) { shift = false; }
            }
        };
    }

    private MouseMotionListener getMotionListener(){
        return new MouseMotionListener() {
            @Override public void mouseDragged(MouseEvent mouseEvent) {
                mouseX = mouseEvent.getX();
                mouseY = mouseEvent.getY();
                onMouseClick();
            }

            @Override public void mouseMoved(MouseEvent mouseEvent) {
                mouseX = mouseEvent.getX();
                mouseY = mouseEvent.getY();
            }
        };
    }

    private MouseListener getMouseListener(){
        return new MouseListener() {
            @Override public void mouseReleased(MouseEvent mouseEvent) {
                mouseButton = false;
                onMouseClick();
            }

            @Override public void mouseClicked(MouseEvent mouseEvent) {}
            @Override public void mousePressed(MouseEvent mouseEvent) {
                mouseButton = true;
            }
            @Override public void mouseEntered(MouseEvent mouseEvent) {
                mouseInWindow = true;
            }
            @Override public void mouseExited(MouseEvent mouseEvent) {
                mouseInWindow = false;
            }
        };
    }

    private void initThreads(){
        new InfiniteLoopThread() {
            @Override
            public void task() {
                System.out.print("> ");
                Scanner in = new Scanner(System.in);
                String text = in.nextLine();

                if(text.toLowerCase(Locale.ROOT).equals("help")){
                    System.out.println(
                            "\nhelp - display help" +
                                    "\nsel material - select material" +
                                    "\nexp - export map");
                } else if (text.toLowerCase(Locale.ROOT).contains("sel")) {
                    String[] cmd = text.split(" ");
                    material = cmd[1];
                }
            }
        }.start();

        new InfiniteLoopThread() {
            @Override
            public void task() throws Exception {
                foreground = stringToIMG("FPS: " + fps);
                Thread.sleep(50);
            }
        }.start();

        new InfiniteLoopThread() {
            @Override
            public void task() throws Exception {
                Thread.sleep(8);
                while (!TouchIDs.isEmpty()) try {
                    sprites.get(TouchIDs.poll()).onTouch();
                } catch (NullPointerException ex) {TouchIDs.clear();}
            }
        }.start();

        new InfiniteLoopThread(){
            @Override public void task() throws Exception {
                Thread.sleep(8); // 120 раз в секунду
                updatePos(); // обновляем позицию игрока
            }
        }.start();

        new InfiniteLoopThread(){
            @Override public void task() throws Exception {
                Thread.sleep(50); // 20 раз в секунду
                updateTextures(); // обновляем анимированные текстуры
            }
        }.start();

        new InfiniteLoopThread(){
            @Override public void task() throws Exception {
                Thread.sleep(1000); // каждую секунду
                fps = frames; // количество сделанных кадров = кадры в секунду
                frames = 0; // обнуляем кол-во готовых кадров
                //System.out.println(Main.e.debugInfo()); // и выводим фпс в консоль
            }
        }.start();
    }
}