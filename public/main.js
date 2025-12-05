

function applySchedulerConfig() {
  const strategy = document.getElementById("strategy-select").value;
  const interval = parseInt(document.getElementById("interval-input").value);

  fetch('/api/scheduler/config', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ strategy, interval })
  })
  .then(res => {
    if (res.ok) alert("Scheduler config applied!");
    else alert("Failed to apply config");
  });
}

// タブ切り替え
function showTab(tabId) {
	document.querySelectorAll(".tab").forEach(btn => btn.classList.remove("active"));
	document.querySelectorAll(".tab-content").forEach(div => div.classList.remove(
		"active"));
	document.querySelector(`.tab[onclick="showTab('${tabId}')"]`).classList.add(
		"active");
	document.getElementById(tabId).classList.add("active");
}

const chartInstances = {};
const memoryChartInstances = {};
const gpuChartInstances = {};

// グラフの真ん中にテキストを表示するプラグイン
const centerTextPlugin = {
	id: 'centerText',
	beforeDraw(chart) {
		const {
			width, height, ctx
		} = chart;
		const dataset = chart.config.data.datasets[0];
		const text = dataset.valueText || ''; // 安全に取得

		ctx.save();
		ctx.font = 'bold 16px sans-serif';
		ctx.fillStyle = '#333';
		ctx.textAlign = 'center';
		ctx.textBaseline = 'middle';
		ctx.fillText(text, width / 2, height / 2);
		ctx.restore();
	}
};

let cpuBenchmarks = {}; // CPUベンチマーク情報
let gpuBenchmarks = {}; // GPUベンチマーク情報

// JSONファイルを読み込む関数
async function loadBenchmarks() {
	const res = await fetch("cpu_benchmarks.json");
	cpuBenchmarks = await res.json();
	const res2 = await fetch("gpu_benchmarks.json");
	gpuBenchmarks = await res2.json();
}

// CPU名からスコアを探す関数
function findCPUBenchmark(cpuName) {
	if (!cpuName || Object.keys(cpuBenchmarks).length === 0) return "Unknown";
	const normalized = cpuName.toLowerCase();

	for (const key in cpuBenchmarks) {
		const keyNormalized = key.toLowerCase();
		if (normalized.includes(keyNormalized) || keyNormalized.includes(normalized)) {
			return cpuBenchmarks[key];
		}
	}

	return "Unknown";
}

// GPU名からスコアを探す関数
function findGPUBenchmark(gpuName) {
	if (!gpuName || Object.keys(gpuBenchmarks).length === 0) return "Unknown";
	const normalized = gpuName.toLowerCase();

	for (const key in gpuBenchmarks) {
		const keyNormalized = key.toLowerCase();
		if (normalized.includes(keyNormalized) || keyNormalized.includes(normalized)) {
			return gpuBenchmarks[key];
		}
	}

	return "Unknown";
}


let DynamicInfo = {};
let StaticInfo = {};
let first = true;
let totalMemory = {};

async function updatePCInfo() {
	const dRes = await fetch(`/dynamic?ts=${Date.now()}`);
	const dynamicData = await dRes.json();
	if (first || dynamicData.staticInfoChanged) {
		first = false;
		const sRes = await fetch(`/static?ts=${Date.now()}`);
		const staticData = await sRes.json();
		renderStaticHTML(staticData);
		console.log("HTML形成");

		updateDynamicData(dynamicData);
		return;
	}

	console.log("動的情報更新");
	updateDynamicData(dynamicData);

}

function renderStaticHTML(staticData) {
const containerLoad = document.getElementById("dashboard-load");
if(!containerLoad){
	console.log("couldn't find Id(dashboard-load).");
}
containerLoad.innerHTML = "";

const containerCPU = document.getElementById("dashboard-cpu");
if(!containerCPU){
	console.log("couldn't find Id(dashboard-cpu).");
}
containerCPU.innerHTML = "";

const containerMemory = document.getElementById("dashboard-memory");
if(!containerMemory){
	console.log("couldn't find Id(dashboard-memory).");
}
containerMemory.innerHTML = "";

const containerGPU = document.getElementById("dashboard-gpu");
if(!containerGPU){
	console.log("couldn't find Id(dashboard-gpu).");
}
containerGPU.innerHTML = "";

const containerNetwork = document.getElementById("dashboard-network");
if(!containerNetwork){
	console.log("couldn't find Id(dashboard-network).");
}
containerNetwork.innerHTML = "";

	staticData.allIPAddresses.forEach(ip => {
		const staticPCInfo = staticData.staticPCInfos[ip];
		totalMemory[ip] = staticPCInfo.TotalMemory;
		renderLoadAverageCard(ip);
		renderCPUCard(ip, staticPCInfo.CPU);
		renderMemoryCard(ip, staticPCInfo.TotalMemory);
		renderGPUCard(ip, staticPCInfo.GPUs);
		renderNetworkCard(ip, staticPCInfo.NetworkCards);
	});

	renderNetworkSpeedCard();
	return;

};
function renderLoadAverageCard(ip){
	const containerLoad = document.getElementById("dashboard-load");
	if(!containerLoad){
		console.log("couldn't find Id(dashboard-load).");
	}
	const cardLoad = document.createElement("div");
	cardLoad.className = "card";
	cardLoad.innerHTML =
		`
			<div><strong>Agent Count</strong><br><p class="agentNum" data-ip="${ip}"></p></div>
			<div style="margin-top: 0.5em;"><strong>IP:</strong> ${ip}</div>
			<div style="margin-top: 0.5em; font-weight: bold; background: #1c4793; color: white;">Load Average</div>
			<div class="LoadAverage" data-ip="${ip}" style="font-size: 24px; margin-top: 0.3em; color: #ff9800; font-weight: bold;"></div>
		`;
	containerLoad.appendChild(cardLoad);
}

// --- クロック・CPU使用率カード ---
function renderCPUCard(ip, CPU){
	const containerCPU = document.getElementById("dashboard-cpu");
	if(!containerCPU){
		console.log("couldn't find Id(dashboard-cpu).");
	}
	const cardCPU = document.createElement("div");
	const cpuBenchmarkScore = findCPUBenchmark(CPU.Name);
	cardCPU.className = "card";
	cardCPU.innerHTML =
		`
			<div><strong>Agent Count</strong><br><p class="agentNum" data-ip="${ip}"></p></div>
			<div><strong>IP:</strong> ${ip}</div>
			<div style="margin-top: 0.5em; font-weight: bold; background: #1c4793; color: white;">${CPU.Name}</div>
			<div><strong>Clock Speed:</strong><p class="cpuClockSpeed" data-ip="${ip}"></p></div>
			<div><strong>Benchmark:</strong> ${cpuBenchmarkScore}</div>
			<div style="margin-top: 0.8em;"><strong>CPU使用率</strong></div>
			<div class="chart-container">
				<canvas class="cpuChart canvas" data-ip="${ip}"></canvas>
			</div>
		`;
	containerCPU.appendChild(cardCPU);
}

function renderMemoryCard(ip, totalMemory){
	// --- メモリ状況表示カード ---
	const containerMemory = document.getElementById("dashboard-memory");
	if(!containerMemory){
		console.log("couldn't find Id(dashboard-memory).");
	}
	const cardMemory = document.createElement("div");
	cardMemory.className = "card";
	cardMemory.innerHTML +=
		`
			<div><strong>Agent Count</strong><br><p class="agentNum" data-ip="${ip}"></p></div>
			<div><strong>IP:</strong> ${ip}</div>
			<div style="margin-top: 0.5em; font-weight: bold; background: #1c4793; color: white;">Memory</div>
			<div><strong>Used Memory:</strong><p class="usedMemory" data-ip="${ip}">${totalMemory}</p></div>
			<div style="margin-top: 0.8em;"><strong>Memory Usage</strong></div>
			<div class="chart-container">
				<canvas class="memoryChart canvas" data-ip="${ip}"></canvas>
			</div>
		`;
	containerMemory.appendChild(cardMemory);
}

// --- GPUカード ---
function renderGPUCard(ip, GPUs){
	const containerGPU = document.getElementById("dashboard-gpu");
	if(!containerGPU){
		console.log("couldn't find Id(dashboard-gpu).");
	}
	const cardGPU = document.createElement("div");
	cardGPU.className = "card";
	// GPU情報のHTMLリストを構築
	let gpuListHTML = "";
	if (GPUs && Array.isArray(GPUs)) {
		GPUs.forEach(gpu => {
			const gpuBenchmarkScore = findGPUBenchmark(gpu.Name);
			const vram = (gpu.VRam / 1_000_000_000).toFixed(2) ||
				0;
			gpuListHTML +=
				`
					<div style="margin-top: 0.5em; font-weight: bold; background: #1c4793; color: white;">${gpu.Name}</div>
					<div class="gpu-info-row">
						<div class="gpu-labels">
							<div>Load:</div>
							<div>VRAM:</div>
							<div>Temp:</div>
							<div>Benchmark:</div>
						</div>
						<div class="gpu-values">
							<div> %</div>
							<div class="gpuMemory" data-ip="${ip}" data-gpu="${gpu.Name}">${vram}GB</div>
							<div class="gpuTemperature" data-ip="${ip}" data-gpu="${gpu.Name}">Unknown</div>
							<div>${gpuBenchmarkScore}</div>
						</div>
					</div>
					<div class="gpu-chart">
						<canvas class="gpuChart canvas" data-ip="${ip}" data-gpu="${gpu.Name}"></canvas>
					</div>
				`;
		});
	}


	cardGPU.innerHTML =
		`
			<div><strong>Agent Count</strong><br><p class="agentNum" data-ip="${ip}"></p></div>
			<div><strong>IP:</strong> ${ip}</div>
			<div style="margin: 0.5em 0; font-weight: bold; background: #1c4793; color: white;">GPU</div>
			${gpuListHTML}
		`;
	containerGPU.appendChild(cardGPU);
}

// --- ネットワーク ---
function renderNetworkCard(ip, Networks){
	const containerNetwork = document.getElementById("dashboard-network");
	if(!containerNetwork){
		console.log("couldn't find Id(dashboard-network).");
	}
	let networkRows = "";
	const cardNetwork = document.createElement("div");
	cardNetwork.className = "card";
	
	// ネットワーク情報の繰り返し部分
	if (Networks && Array.isArray(Networks)) {
		Networks.forEach(networkCard => {
			const display = networkCard.DisplayName || networkCard.Name;
			const rawBandwidth = networkCard.Bandwidth || 0;
			// ビット/秒 → Gbps に変換
			const bandwidthGbps = (rawBandwidth / 1_000_000_000).toFixed(2);
			// 0 の時は表示を工夫
			const displayBandwidth = rawBandwidth === 0 ?
				"<span style='color:gray;'>Unknown</span>" : `${bandwidthGbps} Gbps`;
			networkRows +=
				`
					<tr>
						<td>${display}</td>
						<td class="upload" data-ip="${ip}" data-NC="${networkCard.Name}"></td>
						<td class="download" data-ip="${ip}" data-NC="${networkCard.Name}"></td>
					</tr>
				`;
		});
	}
	let networkHTML =
		`
			<div><strong>Agent Count</strong><br><p class="agentNum" data-ip="${ip}"></p></div>
			<div><strong>IP:</strong> ${ip}</div>

			<!-- 横スクロール可能なテーブルラッパー -->
			<div class="network-table-wrapper">
		  		<table class="network-table">
					<colgroup>
						<col style="width: 34%;">
						<col style="width: 33%;">
						<col style="width: 33%;">
					</colgroup>
					<thead>
						<tr>
							<th>Interface</th>
							<th>Upload</th>
							<th>Download</th>
						</tr>
					</thead>
					<tbody>
						${networkRows}
					</tbody>
				</table>
			</div>
		`;
	cardNetwork.innerHTML += networkHTML;
	containerNetwork.appendChild(cardNetwork);
}

// --- ネットワークスピードカード ---
function renderNetworkSpeedCard(){
	const containerNetworkSpeed = document.getElementById("dashboard-speed");
	if(!containerNetworkSpeed){
		console.log("couldn't find Id(dashboard-speed).");
	}
	containerNetworkSpeed.innerHTML = "";
	const cardNetworkSpeed = document.createElement("div");
	cardNetworkSpeed.className = "card";
	cardNetworkSpeed.innerHTML =
		`
			<table class="network-speed-table">
				<thead>
					<tr>
						<th>送信元 IP</th>
						<th>宛先 IP</th>
						<th>アップロード速度 (Mbps)</th>
						<th>ダウンロード速度 (Mbps)</th>
					</tr>
				</thead>
				<tbody id="NetworkSpeedBody">
				</tbody>
			</table>
		`;
	containerNetworkSpeed.appendChild(cardNetworkSpeed);
}

function updateDynamicData(dynamicData) {
	const containerNetworkSpeed = document.getElementById("NetworkSpeedBody");
	if(!containerNetworkSpeed){
		console.log("couldn't find Id(NetworkSpeedBody).");
	}
	containerNetworkSpeed.innerHTML = "";
	for (const ip in dynamicData.dynamicPCInfos) {
		const dynamicPCInfo = dynamicData.dynamicPCInfos[ip];
		const containerLoadAverage = document.querySelector(
			`.LoadAverage[data-ip="${ip}"]`);
		if(!containerLoadAverage){
			console.log(`couldn't find Id(.LoadAverage{data-ip="${ip}"]).`);
		}
		
		const containerCpuCanvas = document.querySelector(
			`.cpuChart.canvas[data-ip="${ip}"]`);
		if(!containerCpuCanvas){
			console.log(`couldn't find Id(.cpuChart.canvas[data-ip="${ip}"]).`);
		}
		
		const containerCpuClockSpeed = document.querySelector(
			`.cpuClockSpeed[data-ip="${ip}"]`);
		if(!containerCpuClockSpeed){
			console.log(`couldn't find Id(.cpuClockSpeed[data-ip="${ip}"]).`);
		}
		
		const containerMemoryCanvas = document.querySelector(
			`.memoryChart.canvas[data-ip="${ip}"]`);
		if(!containerMemoryCanvas){
			console.log(`couldn't find Id(.memoryChart.canvas[data-ip="${ip}"]).`);
		}
		
		const containerUsedMemory = document.querySelector(
			`.usedMemory[data-ip="${ip}"]`);
		if(!containerUsedMemory){
			console.log(`couldn't find Id(.usedMemory[data-ip="${ip}"]).`);
		}

		document.querySelectorAll(`.agentNum[data-ip="${ip}"]`).forEach(el => {
			el.textContent = dynamicPCInfo.AgentsNum;
		});
		containerLoadAverage.textContent = dynamicPCInfo.LoadAverage.toFixed(2);
		const cpuPercent = (dynamicPCInfo.CPU.LoadPercent * 100).toFixed(2);

		if (chartInstances[ip]) {
			const chart = chartInstances[ip];
			chart.data.datasets[0].data = [cpuPercent, 100 - cpuPercent];
			chart.data.datasets[0].valueText = `${cpuPercent}%`; // 中央表示テキスト
			chart.update(); // 🔁 アニメーション付きで更新
		} else {
			// 初回だけ作成
			chartInstances[ip] = new Chart(containerCpuCanvas.getContext("2d"), {
				type: 'doughnut',
				data: {
					datasets: [{
						data: [cpuPercent, 100 - cpuPercent],
						backgroundColor: ['#f44336', '#eeeeee'],
						borderWidth: 0,
						valueText: `${cpuPercent}%`
					}]
				},
				options: {
					animation: {
						duration: 300, // アニメーション速度
						easing: 'easeOutQuart'
					},
					cutout: "70%",
					plugins: {
						tooltip: {
							enabled: false
						},
						legend: {
							display: false
						}
					}
				},
				plugins: [centerTextPlugin]
			});
		}

		const clockGHz = (dynamicPCInfo.CPU.ClockSpeed / 1_000_000_000).toFixed(2);
		containerCpuClockSpeed.textContent = clockGHz + "GHz";

		// メモリ使用率円グラフ (仮に16GBのうちの空きメモリで想定)
		const usedPercent = Math.round((1 - dynamicPCInfo.FreeMemory / totalMemory[ip]) *
			100);
		if (memoryChartInstances[ip]) {
			// 🔁 再描画せずデータだけ更新
			const chart = memoryChartInstances[ip];
			chart.data.datasets[0].data = [usedPercent, 100 - usedPercent];
			chart.data.datasets[0].valueText = `${usedPercent}%`;
			chart.update(); // 🔧 アニメーション付き更新
		} else {
			// 🆕 初回だけ作成
			memoryChartInstances[ip] = new Chart(containerMemoryCanvas.getContext(
				"2d"), {
				type: 'doughnut',
				data: {
					datasets: [{
						data: [usedPercent, 100 - usedPercent],
						backgroundColor: ['#03a9f4', '#eeeeee'],
						borderWidth: 0,
						valueText: `${usedPercent}%`
					}]
				},
				options: {
					animation: {
						duration: 300, // 更新アニメーションの速さ（ms）
						easing: 'easeOutQuart'
					},
					cutout: "70%",
					plugins: {
						tooltip: {
							enabled: false
						},
						legend: {
							display: false
						}
					}
				},
				plugins: [centerTextPlugin]
			});
		}

		const usedGB = ((totalMemory[ip] - dynamicPCInfo.FreeMemory) / 1024 / 1024 / 1024).toFixed(2); // GB表示（小数2桁）
		containerUsedMemory.textContent = usedGB + "GB";
		if(Object.keys(dynamicPCInfo.GPUs).length > 0){
		dynamicPCInfo.GPUs.forEach(gpu => {

			const containerTemperature = document.querySelector(
				`.gpuTemperature[data-ip="${ip}"][data-gpu="${gpu.Name}"]`);
			if(!containerTemperature){
				console.log(`couldn't find Id(.gpuTemperature[data-ip="${ip}"][data-gpu="${gpu.Name}"]).`);
			}
			const containerMemory = document.querySelector(
				`.gpuMemory[data-ip="${ip}"][data-gpu="${gpu.Name}"]`);
			if(!containerMemory){
				console.log(`couldn't find Id(.gpuMemory[data-ip="${ip}"][data-gpu="${gpu.Name}"]).`);
			}
			const containerGpuCanvas = document.querySelector(
				`.gpuChart.canvas[data-ip="${ip}"][data-gpu="${gpu.Name}"]`);
			if(!containerGpuCanvas){
				console.log(`couldn't find Id(.gpuChart.canvas[data-ip="${ip}"][data-gpu="${gpu.Name}"]).`);
			}
				
			const gpuMemoryContent = gpu.UsedMemory + "MB / " + gpu.TotalMemory + "MB";

			containerTemperature.textContent = gpu.TemperatureC + "℃";
			containerMemory.textContent = gpuMemoryContent;

			const gpuUsedPercent = gpu.LoadPercent;
			if (gpuChartInstances[ip+gpu.Name]) {
						// 🔁 再描画せずデータだけ更新
						const chart = gpuChartInstances[ip+gpu.Name];
						chart.data.datasets[0].data = [gpuUsedPercent, 100 - gpuUsedPercent];
						chart.data.datasets[0].valueText = `${gpuUsedPercent}%`;
						chart.update(); // 🔧 アニメーション付き更新
					} else {
						// 🆕 初回だけ作成
						gpuChartInstances[ip+gpu.Name] = new Chart(containerGpuCanvas.getContext(
							"2d"), {
							type: 'doughnut',
							data: {
								datasets: [{
									data: [gpuUsedPercent, 100 - gpuUsedPercent],
									backgroundColor: ['#4caf50', '#eeeeee'],
									borderWidth: 0,
									valueText: `${gpuUsedPercent}%`
								}]
							},
							options: {
								animation: {
									duration: 300, // 更新アニメーションの速さ（ms）
									easing: 'easeOutQuart'
								},
								cutout: "70%",
								plugins: {
									tooltip: {
										enabled: false
									},
									legend: {
										display: false
									}
								}
							},
							plugins: [centerTextPlugin]
						});
					}

		})}
		Object.entries(dynamicPCInfo.NetworkCards).forEach(([name, networkCard]) => {
			const containerUpload = document.querySelector(
				`.upload[data-ip="${ip}"][data-NC="${name}"]`);
			if(!containerUpload){
				console.log(`couldn't find Id(.upload[data-ip="${ip}"][data-NC="${name}"]).`);
			}
			const containerDownload = document.querySelector(
				`.download[data-ip="${ip}"][data-NC="${name}"]`);
			if(!containerDownload){
				console.log(`couldn't find Id(.download[data-ip="${ip}"][data-NC="${name}"]).`);
			}

			const uploadMbps = ((networkCard.UploadSpeed || 0) * 8 / 1_000_000).toFixed(2);
			const downloadMbps = ((networkCard.DownloadSpeed || 0) * 8 / 1_000_000).toFixed(2);

			if (containerUpload) {
				containerUpload.textContent = `${uploadMbps} Mbps`;
			}
			if (containerDownload) {
				containerDownload.textContent = `${downloadMbps} Mbps`;
			}
		});
		const containerNetworkSpeed = document.getElementById("NetworkSpeedBody");
		if(!containerNetworkSpeed){
			console.log("couldn't find Id(NetworkSpeedBody).");
		}
		dynamicPCInfo.NetworkSpeeds.forEach(networkSpeed => {
			const networkUploadSpeed = networkSpeed.UploadSpeedByOriginal.toFixed(0);
			const networkDownloadSpeed = networkSpeed.DownloadSpeedByOriginal.toFixed(0);
			containerNetworkSpeed.innerHTML += 
				`
				<tr>
					<td>${networkSpeed.Sender}</td>
					<td>${networkSpeed.Receiver}</td>
					<td>${networkUploadSpeed} Mbps</td>
					<td>${networkDownloadSpeed} Mbps</td>
				</tr>
				`;
		});
	}
}


// 最初の読み込み
updatePCInfo();

// 1秒ごとに再取得
let updateInterval = 1000;
setInterval(updatePCInfo, updateInterval);


async function main() {
	await loadBenchmarks();


};

main();
