import type { Env } from "../types.js";
import { htmlResponse } from "../auth.js";
import { getQuestionIdByToken, getQuestion } from "../services/kv.js";

function escapeHtml(str: string): string {
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function renderPage(
  title: string,
  body: string,
  script?: string
): string {
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<title>${escapeHtml(title)}</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    background: #f5f5f5;
    color: #1a1a1a;
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 24px 16px;
  }
  .card {
    background: #fff;
    border-radius: 24px;
    padding: 32px 24px;
    max-width: 480px;
    width: 100%;
    box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  }
  .label {
    font-size: 12px;
    font-weight: 600;
    color: #6750a4;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 8px;
  }
  .question {
    font-size: 20px;
    font-weight: 600;
    line-height: 1.4;
    margin-bottom: 24px;
    color: #1a1a1a;
  }
  .options {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 24px;
  }
  .option-btn {
    display: block;
    width: 100%;
    padding: 16px 20px;
    font-size: 16px;
    font-weight: 500;
    border: 2px solid #e0e0e0;
    border-radius: 16px;
    background: #f8f5ff;
    color: #1a1a1a;
    cursor: pointer;
    transition: all 0.2s;
    text-align: left;
  }
  .option-btn:hover { border-color: #6750a4; background: #ede8f5; }
  .option-btn.selected { border-color: #6750a4; background: #6750a4; color: #fff; }
  .divider {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;
    color: #999;
    font-size: 13px;
  }
  .divider::before, .divider::after {
    content: "";
    flex: 1;
    height: 1px;
    background: #e0e0e0;
  }
  .input-group { margin-bottom: 20px; }
  .input-group textarea {
    width: 100%;
    min-height: 80px;
    padding: 14px 16px;
    font-size: 16px;
    border: 2px solid #e0e0e0;
    border-radius: 16px;
    resize: vertical;
    font-family: inherit;
    outline: none;
    transition: border-color 0.2s;
  }
  .input-group textarea:focus { border-color: #6750a4; }
  .submit-btn {
    display: block;
    width: 100%;
    padding: 16px;
    font-size: 16px;
    font-weight: 600;
    border: none;
    border-radius: 16px;
    background: #6750a4;
    color: #fff;
    cursor: pointer;
    transition: opacity 0.2s;
  }
  .submit-btn:hover { opacity: 0.9; }
  .submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
  .status {
    text-align: center;
    padding: 40px 20px;
  }
  .status .icon { font-size: 48px; margin-bottom: 16px; }
  .status h2 { font-size: 20px; margin-bottom: 8px; }
  .status p { color: #666; font-size: 14px; }
</style>
</head>
<body>
${body}
${script ? `<script>${script}</script>` : ""}
</body>
</html>`;
}

export async function handlePage(
  answerToken: string,
  env: Env,
  workerUrl: string
): Promise<Response> {
  const questionId = await getQuestionIdByToken(env, answerToken);
  if (!questionId) {
    return htmlResponse(
      renderPage(
        "链接已失效",
        `<div class="card"><div class="status">
          <div class="icon">&#128533;</div>
          <h2>链接已失效</h2>
          <p>该问题链接已过期或已被回答。</p>
        </div></div>`
      ),
      404
    );
  }

  const record = await getQuestion(env, questionId);
  if (!record) {
    return htmlResponse(
      renderPage(
        "问题不存在",
        `<div class="card"><div class="status">
          <div class="icon">&#128533;</div>
          <h2>问题不存在</h2>
          <p>该问题可能已过期。</p>
        </div></div>`
      ),
      404
    );
  }

  if (record.status === "answered") {
    return htmlResponse(
      renderPage(
        "已回答",
        `<div class="card"><div class="status">
          <div class="icon">&#9989;</div>
          <h2>已回答</h2>
          <p>你已回答过这个问题。</p>
        </div></div>`
      )
    );
  }

  let optionsHtml = "";
  if (record.options && record.options.length > 0) {
    optionsHtml = `
      <div class="options" id="options">
        ${record.options.map((opt) => `<button class="option-btn" onclick="selectOption(this)">${escapeHtml(opt)}</button>`).join("")}
      </div>
      <div class="divider">或自由输入</div>
    `;
  }

  const bodyHtml = `
    <div class="card">
      <div class="label">Claude 的问题</div>
      <div class="question">${escapeHtml(record.question)}</div>
      ${optionsHtml}
      <div class="input-group">
        <textarea id="answer" placeholder="输入你的回答..."></textarea>
      </div>
      <button class="submit-btn" id="submitBtn" onclick="submitAnswer()">提交回答</button>
    </div>
  `;

  const script = `
    var selectedOption = null;
    var questionId = "${questionId}";
    var answerToken = "${answerToken}";
    var workerUrl = "${workerUrl}";

    function selectOption(btn) {
      document.querySelectorAll('.option-btn').forEach(function(b) { b.classList.remove('selected'); });
      btn.classList.add('selected');
      selectedOption = btn.textContent;
      document.getElementById('answer').value = '';
    }

    function submitAnswer() {
      var textarea = document.getElementById('answer');
      var answer = textarea.value.trim() || selectedOption;
      if (!answer) { textarea.focus(); return; }

      var btn = document.getElementById('submitBtn');
      btn.disabled = true;
      btn.textContent = '提交中...';

      fetch(workerUrl + '/answer/' + questionId, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ answer: answer, answer_token: answerToken })
      })
      .then(function(r) { return r.json(); })
      .then(function(data) {
        if (data.status === 'ok') {
          document.querySelector('.card').innerHTML =
            '<div class="status"><div class="icon">&#9989;</div>' +
            '<h2>回答已提交</h2><p>Claude 将继续工作。</p></div>';
        } else {
          btn.disabled = false;
          btn.textContent = '提交回答';
          alert('提交失败: ' + (data.error || '未知错误'));
        }
      })
      .catch(function(err) {
        btn.disabled = false;
        btn.textContent = '提交回答';
        alert('网络错误，请重试');
      });
    }
  `;

  return htmlResponse(renderPage("回答 Claude 的问题", bodyHtml, script));
}
