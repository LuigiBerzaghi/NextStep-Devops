import com.google.genai.types.GenerateContentConfig;
var builder = GenerateContentConfig.builder();
for (var m : builder.getClass().getMethods()) {
    if (m.getDeclaringClass().getPackageName().contains("genai")) {
        System.out.println(m.getName() + java.util.Arrays.toString(m.getParameterTypes()));
    }
}

