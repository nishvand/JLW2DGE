package nishvand.World;

import nishvand.Objects.Sprite;

import java.util.ArrayList;
import java.util.Random;

import static nishvand.Engine.createSprite;
import static nishvand.Engine.findImage;

public class Structure {
    String type;
    Random rnd;

    public Structure(String type){
        this.type = type;
        rnd = new Random();
    }

    public ArrayList<Sprite> compile() {
        ArrayList<Sprite> sprites = new ArrayList<>();
        if(type.equals("start")){
            Sprite floor = createSprite(findImage("nether_brick"));
            floor.upscale(4);
            floor.createMatrix(9, true);
            floor.addAttribute("floor");
            floor.createShadow();
            floor.offsetX = 200; floor.offsetY = 200;
            sprites.add(floor);
            sprites.add(floor);
            for (int i = 0; i <= 8; i++){
                for (int j = 0; j <= 8; j++){
                    if((j == 8 || i == 8 || j == 0 || i == 0) && !((j == 4 || j == 5 || j == 3) && i == 8)) {
                        Sprite temp = createSprite(findImage(rnd.nextBoolean() ? "cobblestone" : "cobblestone_mossy"),
                                i * 64 + 200, j * 64 + 200, true);
                        temp.upscale(4);
                        sprites.add(temp);
                    }
                }
            }
        }
        return sprites;
    }
}