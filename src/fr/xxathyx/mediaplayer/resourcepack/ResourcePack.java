package fr.xxathyx.mediaplayer.resourcepack;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.json.simple.JSONObject;

import com.google.gson.Gson;

import dev.jeka.core.api.file.JkPathTree;
import fr.xxathyx.mediaplayer.Main;
import fr.xxathyx.mediaplayer.video.Video;
import fr.xxathyx.mediaplayer.video.data.VideoData;

/**
 * The ResourcePack class creates a resource pack based on a base resource pack and adds
 * audio files and sound definitions for the video player.
 */
public class ResourcePack {
	
	private final Main plugin = Main.getPlugin(Main.class);
	
	// Ruta absoluta a tu resourcepack base
	private final File baseResourcePackZip = new File("C:\\Users\\ignac\\Downloads\\PREPARACION SERVER MINECRAFT\\plugins\\ResourcePackManager\\output\\ResourcePackManager_RSP.zip");
	
	public void create(Video video) {
		try {
			createWithBase(video);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void createWithBase(Video video) throws IOException {
		if(!video.hasAudio()) return;
		
		VideoData videoData = video.getVideoData();
		
		// Carpeta temporal para extraer el base resourcepack
		File tempDir = new File(videoData.getResourcePacksFolder(), video.getName() + "_temp");
		if(tempDir.exists()) deleteDirectory(tempDir);
		tempDir.mkdirs();
		
		// 1. Descomprimir el resourcepack base en tempDir
		unzip(baseResourcePackZip, tempDir);
		
		// 2. Copiar el icono pack.png (opcional, puede quedar del base)
		try {
			Image pack = ImageIO.read(Main.class.getResource("resources/audio.png"));
			BufferedImage bufferedPack = (BufferedImage) pack;
			ImageIO.write(bufferedPack, "png", new File(tempDir, "pack.png"));
		}catch (IOException e) {
			// No crítico si falla
		}
		
		// 3. Crear carpeta para sonidos y copiar audios
		File assets = new File(tempDir, "assets/minecraft/sounds/mediaplayer/");
		assets.mkdirs();
		
		for(int i = 0; i < video.getAudioChannels(); i++) {
			Files.copy(new File(video.getAudioFolder(), i + ".ogg").toPath(),
					   new File(assets, i + ".ogg").toPath(),
					   StandardCopyOption.REPLACE_EXISTING);
		}
		
		// 4. Crear archivo sounds.json con las definiciones de sonidos
		File soundsFile = new File(tempDir, "assets/minecraft/sounds.json");
		
		Map<String, Map<String, Object>> soundsMap = new HashMap<>();
		for(int i = 0; i < video.getAudioChannels(); i++) {
			Map<String, Object> submab = new HashMap<>();
			List<Map<String, Object>> param = new ArrayList<>();
			
			Map<String, Object> inner = new HashMap<>();
			inner.put("name", "mediaplayer/" + i);
			inner.put("preload", true);
			param.add(inner);
			
			submab.put("sounds", param);
			soundsMap.put("mediaplayer." + i, submab);
		}
		
		try (Writer writer = new FileWriter(soundsFile)) {
			new Gson().toJson(soundsMap, writer);
		}
		
		// 5. Crear pack.mcmeta si no existe
		File metaFile = new File(tempDir, "pack.mcmeta");
		if(!metaFile.exists()) {
			JSONObject metaJSON = new JSONObject();
	        metaJSON.put("pack_format", getResourcePackFormat());
	        metaJSON.put("description", "Audio addon for " + video.getName());
	         
	        JSONObject packObject = new JSONObject(); 
	        packObject.put("pack", metaJSON);
	        
	        try (FileWriter file = new FileWriter(metaFile)) {
	            file.write(packObject.toJSONString()); 
	            file.flush();
	        }catch (IOException e) {
	            e.printStackTrace();
	        }
		}
		
		// 6. Comprimir todo en un nuevo zip
		File outputZip = new File(videoData.getResourcePacksFolder(), video.getName() + ".zip");
		zipDirectory(tempDir, outputZip);
		
		// 7. Eliminar carpeta temporal
		deleteDirectory(tempDir);
	}
	
	private void unzip(File zipFile, File destDir) throws IOException {
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				File newFile = new File(destDir, entry.getName());
				if(entry.isDirectory()) {
					newFile.mkdirs();
				} else {
					newFile.getParentFile().mkdirs();
					try (FileOutputStream fos = new FileOutputStream(newFile)) {
						byte[] buffer = new byte[1024];
						int len;
						while ((len = zis.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
						}
					}
				}
				zis.closeEntry();
			}
		}
	}
	
	private void zipDirectory(File folder, File zipFile) throws IOException {
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
			Path sourcePath = folder.toPath();
			Files.walk(sourcePath).filter(path -> !Files.isDirectory(path)).forEach(path -> {
				ZipEntry zipEntry = new ZipEntry(sourcePath.relativize(path).toString().replace("\\", "/"));
				try {
					zos.putNextEntry(zipEntry);
					Files.copy(path, zos);
					zos.closeEntry();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
	}
	
	private void deleteDirectory(File dir) throws IOException {
		if (dir.isDirectory()) {
			for (File file : dir.listFiles()) {
				deleteDirectory(file);
			}
		}
		dir.delete();
	}
	
	public int getResourcePackFormat() {
        if(plugin.getServerVersion().equals("v1_21_R4")) return 55;
        if(plugin.getServerVersion().equals("v1_21_R3")) return 46;
        if(plugin.getServerVersion().equals("v1_21_R2")) return 42;
        if(plugin.getServerVersion().equals("v1_21_R1")) return 34;
        if(plugin.getServerVersion().equals("v1_20_R4")) return 31;
        if(plugin.getServerVersion().equals("v1_20_R3")) return 23;
        if(plugin.getServerVersion().equals("v1_20_R2")) return 18;
        if(plugin.getServerVersion().equals("v1_20_R1")) return 15;
        if(plugin.getServerVersion().equals("v1_19_R3")) return 13;
        if(plugin.getServerVersion().equals("v1_19_R2")) return 12;
        if(plugin.getServerVersion().equals("v1_19_R1")) return 9;
        if(plugin.getServerVersion().equals("v1_18_R1") || plugin.getServerVersion().equals("v1_18_R2")) return 8;
        if(plugin.getServerVersion().equals("v1_17_R1")) return 7;
        if(plugin.getServerVersion().equals("v1_16_R2") || plugin.getServerVersion().equals("v1_16_R3")) return 6;
        if(plugin.getServerVersion().equals("v1_15_R1") || plugin.getServerVersion().equals("v1_16_R1")) return 5;
        if(plugin.getServerVersion().equals("v1_13_R1") || plugin.getServerVersion().equals("v1_13_R2") || plugin.getServerVersion().equals("v1_14_R1")) return 4;
        if(plugin.getServerVersion().equals("v1_11_R1") || plugin.getServerVersion().equals("v1_12_R1")) return 3;
        if(plugin.getServerVersion().equals("v1_9_R1") || plugin.getServerVersion().equals("v1_9_R2") || plugin.getServerVersion().equals("v1_10_R1")) return 2;
        if(plugin.getServerVersion().equals("v1_8_R1") || plugin.getServerVersion().equals("v1_8_R2") || plugin.getServerVersion().equals("v1_7_R4") || plugin.getServerVersion().equals("v1_7_R3") || plugin.getServerVersion().equals("v1_7_R2") || plugin.getServerVersion().equals("v1_7_R1")) return 1;
		return 8;
	}
}
