package com.marksandspencer.foodshub.pal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@SpringBootApplication
@EnableMongoAuditing
@Slf4j
@EnableCaching
@EnableRetry
@EnableScheduling
public class ProductAttributeListingApplication {

    public static void main(String[] args) {
    	readSecrets();
        SpringApplication.run(ProductAttributeListingApplication.class, args);
    }
    
	private static void readSecrets() {
		log.info("Entering into readSecrets");
		try {
			try (Stream<Path> filePathStream = Files.walk(Paths.get("/home/foodshub"))) {
				filePathStream.forEach(filePath -> {
					log.info("filePath :: " + filePath);
					if (Files.isRegularFile(filePath)) {
						System.setProperty(filePath.getFileName().toFile().getName(), readOneLine(filePath));
						log.debug(filePath.getFileName().toFile().getName() + " --- " + readOneLine(filePath));
					}
				});
			}
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	private static String readOneLine(Path path) {
		try {
			List<String> fileLines = Files.readAllLines(path);
			return fileLines.get(0);
		} catch (IOException e) {
			log.error(e.getMessage());
			return "ERROR";
		}
	}
}