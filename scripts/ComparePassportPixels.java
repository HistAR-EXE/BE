import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Local QA helper: compare decoded pixels, allowing harmless PNG metadata/compression changes. */
class ComparePassportPixels {
    public static void main(String[] args) throws Exception {
        var expected = ImageIO.read(new File(args[0]));
        var actual = ImageIO.read(new File(args[1]));
        if (expected == null || actual == null || expected.getWidth() != 1200 || expected.getHeight() != 1500
                || actual.getWidth() != 1200 || actual.getHeight() != 1500) throw new AssertionError("Incomplete passport image");
        if (!Arrays.equals(expected.getRGB(0, 0, 1200, 1500, null, 0, 1200),
                actual.getRGB(0, 0, 1200, 1500, null, 0, 1200))) throw new AssertionError("Passport pixels changed");
        System.out.println("PASS complete passport attachment matches all 1,800,000 preview pixels");
    }
}
