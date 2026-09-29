package com.ckgd.exception;

/** Falha externa traduzida sem expor token, resposta bruta ou detalhes de infraestrutura. */
public class GitHubApiException extends RuntimeException {
    private final int status;
    public GitHubApiException(int status, String message) {
        super(message);
        this.status = status;
    }
    public int getStatus() { return status; }
}
