// ========================================
// Download JAR
// ========================================
document.getElementById("btn-download").addEventListener("click", function () {
  const link = document.createElement("a");
  link.href = "cliHelper-1.0.jar";
  link.download = "cliHelper-1.0.jar";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
});

// ========================================
// Copy command to clipboard
// ========================================
document.getElementById("btn-copy").addEventListener("click", function () {
  const commandText = document.getElementById("command-text").textContent;
  const label = document.getElementById("copy-label");

  navigator.clipboard.writeText(commandText).then(function () {
    label.textContent = "Copied!";
    setTimeout(function () {
      label.textContent = "Copy";
    }, 2000);
  }).catch(function () {
    // Fallback for older browsers
    const textarea = document.createElement("textarea");
    textarea.value = commandText;
    textarea.style.position = "fixed";
    textarea.style.opacity = "0";
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand("copy");
    document.body.removeChild(textarea);
    label.textContent = "Copied!";
    setTimeout(function () {
      label.textContent = "Copy";
    }, 2000);
  });
});
