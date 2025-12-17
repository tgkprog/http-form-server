function isValidDate(d) {
    if (!/^\d{2}-\d{2}-\d{4}$/.test(d)) return false;
    const [dd, mm, yyyy] = d.split("-").map(Number);
    const dt = new Date(yyyy, mm - 1, dd);
    return dt.getFullYear() === yyyy &&
        dt.getMonth() === mm - 1 &&
        dt.getDate() === dd;
}

function submitForm() {
    const name = document.getElementById("name").value.trim();
    const date = document.getElementById("date").value.trim();

    if (!isValidDate(date)) {
        alert("Invalid date. Use dd-mm-yyyy");
        return;
    }

    // Use FormData to properly handle file uploads with multipart/form-data
    const formData = new FormData();
    formData.append("name", name);
    formData.append("date", date);
    formData.append("comment", document.getElementById("comment").value);

    // Add files if selected
    const f1 = document.querySelector('input[name="f1"]');
    const f2 = document.querySelector('input[name="f2"]');
    if (f1.files[0]) formData.append("f1", f1.files[0]);
    if (f2.files[0]) formData.append("f2", f2.files[0]);

    fetch("/", {
        method: "POST",
        headers: {
            "client-key": "SDLKAS2323KD"
            // Note: Do NOT set Content-Type - browser sets it automatically with boundary
        },
        body: formData
    })
        .then(r => r.text())
        .then(t => document.getElementById("result").innerText = t);
}
