package org.occidere.youtubeaudioextractor.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;

@Slf4j
@org.springframework.stereotype.Controller
public class Controller {

	private final String AUDIO_PATH = "/tmp/mp3";

	@RequestMapping("/")
	public String index() {
		return "index.html";
	}

	@PostMapping("/download")
	public void download(HttpServletResponse response, @RequestParam("url") String url) {
		log.info("URL: " + url);

		try {
			File file = getAudioFile(url, "aac");

			if(file.exists() == false) {
				log.error("No File : " + file.getAbsolutePath());
				return;
			}

			String encodedFileName = URLEncoder.encode(file.getName(), "UTF-8");
			log.info("Encoded FileName : " + encodedFileName);

			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/octet-stream");
			response.setHeader("Content-Disposition", "attachment; charset=UTF-8; filename=\"" + encodedFileName + "\"");
			response.setContentLength((int) file.length());

			InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
			FileCopyUtils.copy(inputStream, response.getOutputStream());

		} catch (Exception e) {
			e.printStackTrace();
		}

		log.info("Done");
	}

	private String getFileName(String url, String ext) throws Exception {
		String getTitleCommand = "youtube-dl -e " + url;
		Process proc = Runtime.getRuntime().exec(getTitleCommand);

		String title = String.join("", IOUtils.readLines(proc.getInputStream(), "UTF-8"))
				.replaceAll(" ", "_")
				.replaceAll("/", "");
		log.info("Title : " + title);

		proc.destroy();

		String name = title + "." + ext;
		log.info("Name : " + name);

		return name;
	}

	private File getAudioFile(String url, String ext) throws Exception {
		String name = getFileName(url, ext);

		if(hitCache(name)) {
			log.info("Cache Hit!");
		} else {
			log.info("No Cache");
			String command = String.format("youtube-dl -x --audio-quality 0 --audio-format %s -o %s/%s %s", ext, AUDIO_PATH, name, url);
			log.info("Command : " + command);

			Process proc = Runtime.getRuntime().exec(command);
			proc.waitFor();
			proc.destroy();
		}

		return new File(AUDIO_PATH + "/" + name);
	}

	private boolean hitCache(String fileName) {
		new File(AUDIO_PATH).mkdirs();
		return new File(AUDIO_PATH + "/" + fileName).exists();
	}
}
