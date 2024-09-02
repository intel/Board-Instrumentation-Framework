/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package kutch.biff.marvin.control;
import javafx.scene.paint.Color;
public class GradientColor {
    private final Color color;
    private final float weight;

    public GradientColor(Color color, float weight) {
        this.color = color;
        this.weight = weight;
    }

    public Color getColor() {
        return color;
    }

    public float getWeight() {
        return weight;
    }
}
