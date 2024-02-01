import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DownloadZip {
    public static void main(String[] args) {
        // Crear la lista de URLs a descargar
        List<String> urls = new ArrayList<>();

        Scanner sc = new Scanner(System.in);

        // Pedir al usuario que introduzca las URLs
        System.out.println("Introduce una URL a descargar (por ejemplo http://www.google.com) y pulsa ENTER para añadir otra URL.\nCuando hayas terminado, pulsa ENTER sin escribir nada:");
        String newURL = sc.nextLine();
        while (!newURL.isEmpty()) {
            urls.add(newURL);
            newURL = sc.nextLine();
        }

        // Crear el cliente HTTP
        HttpClient client = HttpClient.newHttpClient();

        // Crear una lista de futuros para almacenar las peticiones HTTP
        List<Future<String>> futures = new ArrayList<>();

        // Enviar las peticiones HTTP y almacenar los futuros
        for (String url : urls) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        // Esperar a que todas las peticiones HTTP se completen
        while (!futures.stream().allMatch(Future::isDone)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // Crear el archivo ZIP
        String zipFileName = "webpages.zip";
        try (FileOutputStream fos = new FileOutputStream(zipFileName);
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            // Añadir cada página web al archivo ZIP
            for (int i = 0; i < urls.size(); i++) {
                String url = urls.get(i);
                String response = null;
                try {
                    response = futures.get(i).get();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }

                ZipEntry zipEntry = new ZipEntry(url + ".html");
                zipOut.putNextEntry(zipEntry);
                zipOut.write(response.getBytes());
                zipOut.closeEntry();
            }

            // Cerrar el archivo ZIP
            zipOut.close();

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Mostrar un mensaje de confirmación
        System.out.println("Las páginas web se han descargado y comprimido en el archivo " + zipFileName);
    }
}
