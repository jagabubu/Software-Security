package com.northbridge.casevault.service;

import java.nio.file.Path;

public record EvidenceDownload(String filename, String contentType, Path path) { }