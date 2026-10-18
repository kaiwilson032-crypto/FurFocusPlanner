BUILD THE APK FOR FREE WITH GITHUB ACTIONS

1. Create a GitHub account and sign in.
2. Click + (top right) > New repository.
3. Name it FurFocusPlanner. Choose Public (GitHub Free includes Actions minutes for public repositories). Tick Add a README only if you are NOT uploading this project's files as the first commit; simplest is to leave all initialise options unticked.
4. Open the new empty repository. Choose Add file > Upload files.
5. Extract FurFocusPlanner_GitHub_Actions.zip on your computer first. Upload the CONTENTS of the extracted folder, including the hidden .github folder. If Windows doesn't show .github, enable View > Show > Hidden items in File Explorer. Do not upload the ZIP itself.
6. Commit the uploaded files to the main branch.
7. Open the repository's Actions tab. If GitHub asks to enable Actions, enable them.
8. A workflow named "Build Fur & Focus Planner APK" should run automatically after the commit. If it does not, open that workflow and choose Run workflow.
9. Wait for the run to finish. Green tick = build succeeded; red cross = build failed.
10. Open the successful run. At the bottom, under Artifacts, click "Fur-Focus-Planner-debug-APK" to download a ZIP. Extract it to get app-debug.apk.
11. Transfer app-debug.apk to your Samsung, open it, and allow installation from that source if Android asks.

IMPORTANT
- This creates a DEBUG APK for testing, not a Play Store release.
- Keep the repository public if using GitHub Free, and do not upload personal client data, passwords, API keys, or real booking information. The source project itself contains no real client records.
- The build may reveal code/configuration errors. If it fails, open the failed run, click the build job, and copy the first actual error (not just the final BUILD FAILED line) back into ChatGPT.
