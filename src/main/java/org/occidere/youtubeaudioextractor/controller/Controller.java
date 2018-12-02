package org.occidere.youtubeaudioextractor.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.NoSuchFileException;

@Slf4j
@org.springframework.stereotype.Controller
public class Controller {
	Logger controllerLogger = LoggerFactory.getLogger("logger.controller");

	@Value("${audio.path}")
	private String audioPath;
	@Value("${index.page}")
	private String indexPage;

	@RequestMapping("/")
	public String index() {
		return indexPage;
	}

	@PostMapping("/download")
	public void download(HttpServletRequest request, HttpServletResponse response, @RequestParam("url") String url, @RequestParam("audio_format") String ext) {
		controllerLogger.info("Request from: {}", request.getRemoteAddr());
		controllerLogger.info("URL: {}, ext: {}", url, ext);

		try {
			File file = getAudioFile(url, ext);

			if(file.exists() == false) {
				controllerLogger.error("No File : " + file.getAbsolutePath());
				throw new NoSuchFileException(file.getAbsolutePath());
			}

			String encodedFileName = URLEncoder.encode(file.getName(), "UTF-8");
			controllerLogger.info("Encoded FileName : " + encodedFileName);

			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/octet-stream");
			response.setHeader("Content-Disposition", "attachment; charset=UTF-8; filename=\"" + encodedFileName + "\"");
			response.setContentLength((int) file.length());

			InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
			FileCopyUtils.copy(inputStream, response.getOutputStream());

			controllerLogger.info("Done");
		} catch (Exception e) {
			controllerLogger.error("", e);
			throw new RuntimeException(e);
		}
	}

	private String getFileName(String url, String ext) throws Exception {
		Process proc = Runtime.getRuntime().exec("youtube-dl -e " + url);
		String title = String.join("", IOUtils.readLines(proc.getInputStream(), "UTF-8"))
				.replaceAll(" ", "_")
				.replaceAll("/", "");
		proc.destroy();

		String name = title + "." + ext;
		controllerLogger.info("Name : " + name);

		return name;
	}

	private File getAudioFile(String url, String ext) throws Exception {
		String name = getFileName(url, ext);

		if(hitCache(name)) {
			controllerLogger.info("Cache Hit!");
		} else {
			controllerLogger.info("No Cache");
			String command = buildCommand(url, audioPath, name, ext);
			controllerLogger.info("Command : " + command);

			Process proc = Runtime.getRuntime().exec(command);
			proc.waitFor();
			proc.destroy();
		}

		return new File(audioPath + "/" + name);
	}

	private boolean hitCache(String fileName) {
		new File(audioPath).mkdirs();
		return new File(audioPath + "/" + fileName).exists();
	}

	public static String buildCommand(String url, String path, String name, String ext) {
		return String.format("youtube-dl -x --audio-quality 0 --audio-format %s -o %s/%s %s", ext, path, name, url);
	}
}
