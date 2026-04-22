package com.messenger.api;

import com.messenger.models.Conversation;
import com.messenger.models.Message;
import com.messenger.models.User;
import com.messenger.services.MessageService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * HTTP API — маршрутизатор запитів.
 *
 * Маршрути:
 *   POST   /users                              — створити користувача
 *   GET    /users                              — список користувачів
 *   POST   /conversations                      — створити розмову
 *   GET    /conversations                      — список розмов
 *   POST   /messages                           — надіслати повідомлення
 *   GET    /conversations/{id}/messages        — отримати історію
 *   PATCH  /messages/{id}/status               — оновити статус (Варіант 2)
 */
public class Router {

    private final MessageService service;
    private final HttpServer server;

    public Router(MessageService service, int port) throws IOException {
        this.service = service;
        this.server  = HttpServer.create(new InetSocketAddress(port), 0);
        registerRoutes();
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущено на порту " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
    }

    private void registerRoutes() {
        server.createContext("/",              this::handleRoot);
        server.createContext("/users",         this::handleUsers);
        server.createContext("/conversations", this::handleConversations);
        server.createContext("/messages",      this::handleMessages);
    }

    // ── / (головна сторінка) ──────────────────────────────────────────────────

    private void handleRoot(HttpExchange ex) throws IOException {
        if (!ex.getRequestURI().getPath().equals("/")) {
            sendJson(ex, 404, error("Маршрут не знайдено: " + ex.getRequestURI().getPath()));
            return;
        }
        String html = "<!DOCTYPE html><html lang='uk'><head><meta charset='utf-8'>"
+ "<title>Messenger API</title><style>"
+ "*{box-sizing:border-box;margin:0;padding:0}"
+ "body{font-family:monospace;background:#0d1117;color:#c9d1d9;display:flex;height:100vh}"
+ "aside{width:220px;background:#161b22;border-right:1px solid #30363d;padding:1rem;flex-shrink:0}"
+ "aside h2{color:#58a6ff;font-size:.8rem;text-transform:uppercase;letter-spacing:.1em;margin-bottom:.8rem}"
+ ".nav-btn{display:block;width:100%;text-align:left;padding:.5rem .7rem;margin:.2rem 0;"
+ "background:none;border:1px solid transparent;border-radius:6px;color:#8b949e;cursor:pointer;font-family:monospace;font-size:.85rem}"
+ ".nav-btn:hover{background:#21262d;border-color:#30363d;color:#c9d1d9}"
+ ".nav-btn.active{background:#1f3a5f;border-color:#58a6ff;color:#58a6ff}"
+ ".badge{float:right;font-size:.7rem;padding:.1rem .4rem;border-radius:4px;font-weight:bold}"
+ ".post{background:#2d4a1e;color:#56d364}.get{background:#1a3a5c;color:#58a6ff}"
+ ".patch{background:#3d2a0a;color:#e3b341}"
+ "main{flex:1;display:flex;flex-direction:column;overflow:hidden}"
+ "header{padding:1rem 1.5rem;border-bottom:1px solid #30363d;background:#161b22}"
+ "header h1{font-size:1.1rem;color:#f0f6fc}header p{font-size:.8rem;color:#8b949e;margin-top:.2rem}"
+ ".workspace{flex:1;display:grid;grid-template-columns:1fr 1fr;overflow:hidden}"
+ ".panel{padding:1.2rem;overflow-y:auto;display:flex;flex-direction:column;gap:.8rem}"
+ ".panel:first-child{border-right:1px solid #30363d}"
+ "label{font-size:.75rem;color:#8b949e;display:block;margin-bottom:.3rem}"
+ "input,select,textarea{width:100%;padding:.5rem .7rem;background:#0d1117;border:1px solid #30363d;"
+ "border-radius:6px;color:#c9d1d9;font-family:monospace;font-size:.85rem}"
+ "input:focus,textarea:focus{outline:none;border-color:#58a6ff}"
+ "textarea{resize:vertical;min-height:80px}"
+ ".send-btn{padding:.6rem 1.2rem;background:#238636;border:none;border-radius:6px;"
+ "color:#fff;cursor:pointer;font-family:monospace;font-weight:bold;font-size:.9rem}"
+ ".send-btn:hover{background:#2ea043}"
+ ".response{background:#0d1117;border:1px solid #30363d;border-radius:6px;padding:.8rem;"
+ "font-size:.8rem;white-space:pre-wrap;word-break:break-all;min-height:120px;color:#c9d1d9}"
+ ".status-ok{color:#56d364}.status-err{color:#f85149}"
+ ".url-bar{padding:.4rem .7rem;background:#21262d;border:1px solid #30363d;border-radius:6px;"
+ "font-size:.8rem;color:#8b949e;margin-bottom:.3rem}"
+ ".section-title{font-size:.75rem;font-weight:bold;color:#8b949e;text-transform:uppercase;letter-spacing:.05em}"
+ ".id-row{display:flex;gap:.5rem;align-items:center}"
+ ".id-row input{flex:1}"
+ ".copy-btn{padding:.4rem .6rem;background:#21262d;border:1px solid #30363d;border-radius:6px;"
+ "color:#8b949e;cursor:pointer;font-size:.75rem;white-space:nowrap}"
+ ".copy-btn:hover{color:#c9d1d9}"
+ "</style></head><body>"

+ "<aside><h2>Маршрути</h2>"
+ "<button class='nav-btn active' onclick='show(\"createUser\")'><span class='badge post'>POST</span>/users</button>"
+ "<button class='nav-btn' onclick='show(\"getUsers\")'><span class='badge get'>GET</span>/users</button>"
+ "<button class='nav-btn' onclick='show(\"createConv\")'><span class='badge post'>POST</span>/conversations</button>"
+ "<button class='nav-btn' onclick='show(\"getConvMsgs\")'><span class='badge get'>GET</span>/conversations/{id}/messages</button>"
+ "<button class='nav-btn' onclick='show(\"sendMsg\")'><span class='badge post'>POST</span>/messages</button>"
+ "<button class='nav-btn' onclick='show(\"updateStatus\")'><span class='badge patch'>PATCH</span>/messages/{id}/status</button>"
+ "</aside>"

+ "<main>"
+ "<header><h1>Messenger API — Тестовий інтерфейс</h1>"
+ "<p>Варіант 2 — Відстеження статусів повідомлень</p></header>"
+ "<div class='workspace'>"
+ "<div class='panel' id='left-panel'></div>"
+ "<div class='panel'>"
+ "<div class='section-title'>Відповідь</div>"
+ "<div class='url-bar' id='url-bar'>—</div>"
+ "<div class='response' id='response'>Оберіть маршрут і натисніть «Надіслати»</div>"
+ "<div style='font-size:.75rem;color:#8b949e;margin-top:.3rem'>Збережені id:</div>"
+ "<div style='display:flex;flex-direction:column;gap:.4rem;margin-top:.3rem'>"
+ "<div class='id-row'><label style='width:90px;flex-shrink:0;margin:0'>User A id</label><input id='savedUserA' placeholder='(буде збережено автоматично)' readonly><button class='copy-btn' onclick='copyId(\"savedUserA\")'>копіювати</button></div>"
+ "<div class='id-row'><label style='width:90px;flex-shrink:0;margin:0'>User B id</label><input id='savedUserB' placeholder='(буде збережено автоматично)' readonly><button class='copy-btn' onclick='copyId(\"savedUserB\")'>копіювати</button></div>"
+ "<div class='id-row'><label style='width:90px;flex-shrink:0;margin:0'>Conv id</label><input id='savedConv' placeholder='(буде збережено автоматично)' readonly><button class='copy-btn' onclick='copyId(\"savedConv\")'>копіювати</button></div>"
+ "<div class='id-row'><label style='width:90px;flex-shrink:0;margin:0'>Message id</label><input id='savedMsg' placeholder='(буде збережено автоматично)' readonly><button class='copy-btn' onclick='copyId(\"savedMsg\")'>копіювати</button></div>"
+ "</div></div></div></main>"

+ "<script>"
+ "const panels={"

+ "createUser:`<div class='section-title'>POST /users</div>"
+ "<label>Ім'я користувача</label>"
+ "<input id='uname' value='Аліса' />"
+ "<button class='send-btn' onclick='createUser()'>Надіслати</button>`,"

+ "getUsers:`<div class='section-title'>GET /users</div>"
+ "<p style='font-size:.85rem;color:#8b949e'>Повертає список усіх користувачів</p>"
+ "<button class='send-btn' onclick='getUsers()'>Надіслати</button>`,"

+ "createConv:`<div class='section-title'>POST /conversations</div>"
+ "<label>Тип розмови</label>"
+ "<select id='ctype'><option value='direct'>direct</option><option value='group'>group</option></select>"
+ "<button class='send-btn' onclick='createConv()'>Надіслати</button>`,"

+ "getConvMsgs:`<div class='section-title'>GET /conversations/{id}/messages</div>"
+ "<label>Conversation ID</label>"
+ "<input id='gcid' placeholder='вставте id розмови або скопіюйте з панелі' />"
+ "<button class='send-btn' onclick='getMsgs()'>Надіслати</button>`,"

+ "sendMsg:`<div class='section-title'>POST /messages</div>"
+ "<label>Conversation ID</label><input id='mconvid' placeholder='id розмови' />"
+ "<label>Sender ID</label><input id='msenderid' placeholder='id відправника' />"
+ "<label>Текст повідомлення</label><textarea id='mtext'>Привіт!</textarea>"
+ "<button class='send-btn' onclick='sendMsg()'>Надіслати</button>`,"

+ "updateStatus:`<div class='section-title'>PATCH /messages/{id}/status</div>"
+ "<label>Message ID</label><input id='smsgid' placeholder='id повідомлення' />"
+ "<label>Новий статус</label>"
+ "<select id='sstatus'><option value='delivered'>delivered</option><option value='read'>read</option><option value='sent'>sent</option></select>"
+ "<button class='send-btn' onclick='updStatus()'>Надіслати</button>`"
+ "};"

+ "let userCount=0;"
+ "function show(key){"
+ "document.getElementById('left-panel').innerHTML=panels[key];"
+ "document.querySelectorAll('.nav-btn').forEach(b=>b.classList.remove('active'));"
+ "event.target.closest('.nav-btn').classList.add('active');"
+ "}"
+ "show('createUser');"

+ "function setResp(method,url,data,ok){"
+ "document.getElementById('url-bar').textContent=method+' '+url;"
+ "const el=document.getElementById('response');"
+ "el.textContent=JSON.stringify(data,null,2);"
+ "el.className='response '+(ok?'status-ok':'status-err');"
+ "}"

+ "function copyId(id){"
+ "const val=document.getElementById(id).value;"
+ "if(val)navigator.clipboard.writeText(val);"
+ "}"

+ "async function api(method,path,body){"
+ "const opts={method,headers:{'Content-Type':'application/json'}};"
+ "if(body)opts.body=JSON.stringify(body);"
+ "const r=await fetch(path,opts);"
+ "return{ok:r.ok,status:r.status,data:await r.json()};"
+ "}"

+ "async function createUser(){"
+ "const name=document.getElementById('uname').value;"
+ "const r=await api('POST','/users',{name});"
+ "setResp('POST','/users',r.data,r.ok);"
+ "if(r.ok){"
+ "userCount++;"
+ "if(userCount===1)document.getElementById('savedUserA').value=r.data.id;"
+ "else document.getElementById('savedUserB').value=r.data.id;"
+ "document.getElementById('uname').value=userCount===1?'Богдан':'Василь';"
+ "}}"

+ "async function getUsers(){"
+ "const r=await api('GET','/users');"
+ "setResp('GET','/users',r.data,r.ok);}"

+ "async function createConv(){"
+ "const type=document.getElementById('ctype').value;"
+ "const r=await api('POST','/conversations',{type});"
+ "setResp('POST','/conversations',r.data,r.ok);"
+ "if(r.ok){document.getElementById('savedConv').value=r.data.id;"
+ "const ci=document.getElementById('mconvid');if(ci)ci.value=r.data.id;"
+ "const gi=document.getElementById('gcid');if(gi)gi.value=r.data.id;}}"

+ "async function getMsgs(){"
+ "const id=document.getElementById('gcid').value;"
+ "const r=await api('GET','/conversations/'+id+'/messages');"
+ "setResp('GET','/conversations/'+id+'/messages',r.data,r.ok);}"

+ "async function sendMsg(){"
+ "const convId=document.getElementById('mconvid').value;"
+ "const senderId=document.getElementById('msenderid').value||document.getElementById('savedUserA').value;"
+ "const text=document.getElementById('mtext').value;"
+ "const r=await api('POST','/messages',{conversationId:convId,senderId,text});"
+ "setResp('POST','/messages',r.data,r.ok);"
+ "if(r.ok){document.getElementById('savedMsg').value=r.data.id;"
+ "const si=document.getElementById('smsgid');if(si)si.value=r.data.id;}}"

+ "async function updStatus(){"
+ "const id=document.getElementById('smsgid').value;"
+ "const status=document.getElementById('sstatus').value;"
+ "const r=await fetch('/messages/'+id+'/status',{method:'POST',"
+ "headers:{'Content-Type':'application/json','X-HTTP-Method-Override':'PATCH'},"
+ "body:JSON.stringify({status})});"
+ "const data=await r.json();"
+ "setResp('PATCH','/messages/'+id+'/status',data,r.ok);}"
+ "</script></body></html>";

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    // ── /users ────────────────────────────────────────────────────────────────

    private void handleUsers(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        try {
            if ("POST".equalsIgnoreCase(method)) {
                String body  = readBody(ex);
                String name  = extractJsonField(body, "name");
                User user    = service.createUser(name);
                sendJson(ex, 201, user.toString());

            } else if ("GET".equalsIgnoreCase(method)) {
                List<User> users = service.getAllUsers();
                String json = toJsonArray(users.stream()
                    .map(User::toString).collect(Collectors.toList()));
                sendJson(ex, 200, json);

            } else {
                sendJson(ex, 405, error("Метод не підтримується"));
            }
        } catch (IllegalArgumentException e) {
            sendJson(ex, 400, error(e.getMessage()));
        } catch (Exception e) {
            sendJson(ex, 500, error("Внутрішня помилка: " + e.getMessage()));
        }
    }

    // ── /conversations ────────────────────────────────────────────────────────

    private void handleConversations(HttpExchange ex) throws IOException {
        String path   = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        // GET /conversations/{id}/messages
        if ("GET".equalsIgnoreCase(method) && path.matches("/conversations/[^/]+/messages")) {
            String convId = path.split("/")[2];
            try {
                List<Message> messages = service.getMessages(convId);
                String json = toJsonArray(messages.stream()
                    .map(Message::toString).collect(Collectors.toList()));
                sendJson(ex, 200, json);
            } catch (IllegalArgumentException e) {
                sendJson(ex, 404, error(e.getMessage()));
            } catch (Exception e) {
                sendJson(ex, 500, error("Внутрішня помилка: " + e.getMessage()));
            }
            return;
        }

        try {
            if ("POST".equalsIgnoreCase(method)) {
                String body = readBody(ex);
                String type = extractJsonField(body, "type");
                if (type.isBlank()) type = "direct";
                Conversation conv = service.createConversation(type);
                sendJson(ex, 201, conv.toString());

            } else if ("GET".equalsIgnoreCase(method)) {
                List<Conversation> convs = service.getAllConversations();
                String json = toJsonArray(convs.stream()
                    .map(Conversation::toString).collect(Collectors.toList()));
                sendJson(ex, 200, json);

            } else {
                sendJson(ex, 405, error("Метод не підтримується"));
            }
        } catch (IllegalArgumentException e) {
            sendJson(ex, 400, error(e.getMessage()));
        } catch (Exception e) {
            sendJson(ex, 500, error("Внутрішня помилка: " + e.getMessage()));
        }
    }

    // ── /messages ─────────────────────────────────────────────────────────────

    private void handleMessages(HttpExchange ex) throws IOException {
        String path   = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        // Підтримка X-HTTP-Method-Override (обхід обмеження HttpURLConnection)
        String override = ex.getRequestHeaders().getFirst("X-HTTP-Method-Override");
        if (override != null && !override.isBlank()) method = override.toUpperCase();

        // PATCH /messages/{id}/status  (Варіант 2 — оновлення статусу)
        if ("PATCH".equalsIgnoreCase(method) && path.matches("/messages/[^/]+/status")) {
            String msgId = path.split("/")[2];
            try {
                String body   = readBody(ex);
                String status = extractJsonField(body, "status");
                boolean ok    = service.updateStatus(msgId, status);
                if (ok) {
                    sendJson(ex, 200, "{\"updated\":true,\"status\":\"" + status + "\"}");
                } else {
                    sendJson(ex, 404, error("Повідомлення не знайдено"));
                }
            } catch (IllegalArgumentException e) {
                sendJson(ex, 400, error(e.getMessage()));
            } catch (Exception e) {
                sendJson(ex, 500, error("Внутрішня помилка: " + e.getMessage()));
            }
            return;
        }

        try {
            if ("POST".equalsIgnoreCase(method)) {
                String body           = readBody(ex);
                String conversationId = extractJsonField(body, "conversationId");
                String senderId       = extractJsonField(body, "senderId");
                String text           = extractJsonField(body, "text");
                Message msg = service.sendMessage(conversationId, senderId, text);
                sendJson(ex, 201, msg.toString());

            } else {
                sendJson(ex, 405, error("Метод не підтримується"));
            }
        } catch (IllegalArgumentException e) {
            sendJson(ex, 400, error(e.getMessage()));
        } catch (Exception e) {
            sendJson(ex, 500, error("Внутрішня помилка: " + e.getMessage()));
        }
    }

    // ── Утиліти ───────────────────────────────────────────────────────────────

    private String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendJson(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String error(String msg) {
        return "{\"error\":\"" + msg.replace("\"", "\\\"") + "\"}";
    }

    private String toJsonArray(List<String> items) {
        return "[" + String.join(",", items) + "]";
    }

    /**
     * Простий вилучальник поля з JSON (без зовнішніх бібліотек).
     */
    private String extractJsonField(String json, String key) {
        if (json == null) return "";
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        int colon = json.indexOf(":", idx + search.length());
        if (colon < 0) return "";
        int start = json.indexOf("\"", colon + 1);
        if (start < 0) return "";
        start++;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                sb.append(json.charAt(i + 1));
                i++;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
