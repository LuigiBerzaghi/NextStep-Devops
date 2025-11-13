import com.google.genai.types.GenerateContentConfig;
import com.google.genai.Client;
var builder = GenerateContentConfig.builder();
for (var m : builder.getClass().getMethods()) {
    if (m.getDeclaringClass().getPackageName().contains("genai")) {
        System.out.println(m.getName() + java.util.Arrays.toString(m.getParameterTypes()));
    }
}
var clientBuilder = Client.builder();
for (var m : clientBuilder.getClass().getMethods()) {
    if (m.getDeclaringClass().getPackageName().contains("genai")) {
        System.out.println("CLIENT BUILDER -> " + m.getName() + java.util.Arrays.toString(m.getParameterTypes()));
    }
}
