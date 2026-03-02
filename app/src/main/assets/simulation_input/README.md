# Simulation Input Videos

This folder contains cattle videos for simulation mode when no FLIR camera is detected.

## Supported Formats
- MP4 (.mp4)
- AVI (.avi) 
- MOV (.mov)

## Usage
1. Add cattle thermal or regular videos to this folder
2. When the app starts without a FLIR camera, it will automatically use these videos
3. Use the video controls (◀ ▶) in the top-right corner to switch between videos
4. The app will loop each video continuously and extract frames for analysis

## Recommended Videos
- Cattle in various positions (standing, lying, walking)
- Different angles and distances
- Multiple animals in frame
- Clear visibility of body parts (udder, legs, head, body)

## Video Requirements
- Resolution: Any (will be scaled to 640x480 for processing)
- Duration: 10-60 seconds (will loop automatically)
- Quality: Good enough to see cattle body parts clearly

## Example Workflow
1. App detects no FLIR camera → switches to simulation mode
2. Loads videos from this folder → shows video controls
3. User can switch between videos using ◀ ▶ buttons
4. Tap SCAN to capture current frame → sends to backend for analysis
5. Backend runs Grounding DINO detection → returns body part coordinates
6. App generates simulated thermal data → sends for diagnosis

## Notes
- If no videos are found, app falls back to static gray simulation
- Videos play at ~30 FPS simulation
- Each video loops automatically when it reaches the end
- Thermal data is simulated (36-39°C range) for all videos