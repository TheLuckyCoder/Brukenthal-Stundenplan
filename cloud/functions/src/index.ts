import * as functions from 'firebase-functions/v1';
import * as admin from 'firebase-admin';

admin.initializeApp();

/// Constants

const SITE_URL = "https://brukenthal.ro";

/**
 * The keys defined in the Firebase Console -> Remote Config,
 * used to access and store the timetable URLs
 */
const KEY_HIGH_SCHOOL = "url_high_school";
const KEY_MIDDLE_SCHOOL = "url_middle_school";

/**
 * Channel IDs used for sending notifications
 * This need to be the same as in the 'NotificationService' Kotlin class
 */
const CHANNEL_ID_DEFAULT = "notifications";
const CHANNEL_ID_HIGH_SCHOOL = "high_school";
const CHANNEL_ID_MIDDLE_SCHOOL = "middle_school";

/**
 * The titles and messages used for notifications
 */
const TITLES = [
    "Der Stundenplan wurde geändert!",
    "Ein neuer Stundenplan ist da",
    "Achtung, neuer Stundenplan!",
    "Achtung, Stundenplanänderung!",
    "Stundenplan erneut geändert!",
    "Der Stundenplan ist aktualisiert",
    "Ein neuer Stundenplan wurde hochgeladen."
];
const MESSAGES = [
    "Some people call this junk. Me? I call it treasure",
    "Geschwindigkeit und Präzision!",
    "The risk I took was calculated but man am I bad at math",
    "Minim de efort maxim de eficiență",
    "Haide mai bine, să nu",
    "Ani trec robotica rămâne",
    "One does not simply walk into Bruk.",
    "Sunt la școală, scoate-mă de aici!",
    "Let's not get political here",
    "Es ist wie beim bankkollegen, aber es geht nicht.",
    "std::cout << \"Orar nou!\" << std::endl;",
    "Looks like they couldn't handle the Bruk style"
];

/// Structures

/**
 * A small immutable class used for convenience to store the links to the timetables
 */
class ConfigValues {
    readonly highSchool: string
    readonly middleSchool: string

    constructor(highSchoolValue: string, middleSchoolValue: string) {
        this.highSchool = highSchoolValue;
        this.middleSchool = middleSchoolValue;
    }
}

/// Helper Functions

/**
 * Self explanatory, generate a random integer between [min] and [max]
 */
function randomInt(min: number, max: number): number {
    return Math.floor(Math.random() * (max - min + 1) + min);
}

/**
 * Parses a RemoteConfig template into a ConfigValues
 * @param template the template to parse
 */
function processRemoteConfigTemplate(template: admin.remoteConfig.RemoteConfigTemplate): ConfigValues {
    const parameters = template.parameters;

    const highSchoolDefault = (parameters[KEY_HIGH_SCHOOL]?.defaultValue as
        admin.remoteConfig.ExplicitParameterValue)?.value ?? "";
    const middleSchoolDefault = (parameters[KEY_MIDDLE_SCHOOL]?.defaultValue as
        admin.remoteConfig.ExplicitParameterValue)?.value ?? "";

    return new ConfigValues(highSchoolDefault, middleSchoolDefault);
}

/**
 * Updates the Remote Config values
 */
async function updateRemoteConfig(newConfigValues: ConfigValues): Promise<void> {
    console.log("High School Url: " + newConfigValues.highSchool);
    console.log("Middle School Url: " + newConfigValues.middleSchool);

    const config = admin.remoteConfig(); // Get Access to Firebase Remote Config
    const template = await config.getTemplate(); // Get the current template

    const currentValues = processRemoteConfigTemplate(template);

    // Check if the currentValues have changed
    if (currentValues.highSchool !== newConfigValues.highSchool
        || currentValues.middleSchool !== newConfigValues.middleSchool) {

        // Update the template
        template.parameters[KEY_HIGH_SCHOOL] = {
            defaultValue: {value: newConfigValues.highSchool}
        };
        template.parameters[KEY_MIDDLE_SCHOOL] = {
            defaultValue: {value: newConfigValues.middleSchool}
        };

        // Publish the updated template
        try {
            await config.publishTemplate(template);
            console.log("Template has been published");
        } catch (err) {
            console.error("Unable to publish template:", err);
        }
    }
}

/**
 * Fetches HTML from the school website with a browser User-Agent and retry logic
 */
async function fetchTimetableHtml(): Promise<string | null> {
    for (let attempt = 1; attempt <= 2; attempt++) {
        try {
            const response = await fetch(SITE_URL, {
                headers: {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
                    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
                    'Accept-Encoding': 'gzip, deflate, br',
                    'Accept-Language': 'ro-RO,ro;q=0.9,en-US;q=0.8,en;q=0.7'
                },
                signal: AbortSignal.timeout(15000)
            });

            if (response.ok) {
                return await response.text();
            }
            console.warn(`Attempt ${attempt}: ${SITE_URL} returned HTTP ${response.status} ${response.statusText}`);
        } catch (err) {
            console.warn(`Attempt ${attempt} to fetch ${SITE_URL} failed:`, err);
        }

        if (attempt < 2) {
            await new Promise(resolve => setTimeout(resolve, 2000));
        }
    }
    return null;
}

/// Firebase Functions

/**
 * Register the Scheduled Cloud Function
 */
exports.checkForNewTimetable = functions
    .region('europe-west1')
    .runWith({ memory: '512MB' })
    .pubsub
    .schedule('every 50 minutes')
    .onRun(async () => {
        try {
            const html = await fetchTimetableHtml();
            if (!html) {
                console.error("Could not fetch timetable HTML after retries.");
                return;
            }

            // Extract the timetable URLs with regex
            const highSchoolMatch = html.match(/<li[^>]*\bmenu-item-1470\b[^>]*>[\s\S]*?<a\b[^>]*\bhref="([^"]+)"/i);
            const middleSchoolMatch = html.match(/<li[^>]*\bmenu-item-1320\b[^>]*>[\s\S]*?<a\b[^>]*\bhref="([^"]+)"/i);

            if (!highSchoolMatch || !middleSchoolMatch) {
                const candidateLinks = Array.from(
                    new Set([...html.matchAll(/href="([^"]*(?:orar|stundenplan)[^"]*\.pdf)"/gi)].map(m => m[1]))
                );

                console.error([
                    "================================================================================",
                    "[ERROR] School website structure changed! Could not locate timetable URLs.",
                    `Site: ${SITE_URL}`,
                    "Selector status:",
                    `  - High School (menu-item-1470): ${highSchoolMatch ? highSchoolMatch[1] : "NOT FOUND"}`,
                    `  - Middle School (menu-item-1320): ${middleSchoolMatch ? middleSchoolMatch[1] : "NOT FOUND"}`,
                    candidateLinks.length > 0
                        ? `Candidate timetable PDF links discovered on the page:\n${candidateLinks.map(url => `    * ${url}`).join("\n")}`
                        : "No candidate timetable PDF links found on page.",
                    "Action Required: Inspect https://brukenthal.ro and update selectors in cloud/functions/src/index.ts.",
                    "================================================================================"
                ].join("\n"));
                return;
            }

            const highSchoolUrl = highSchoolMatch[1];
            const middleSchoolUrl = middleSchoolMatch[1];

            const newConfigValues = new ConfigValues(
                highSchoolUrl.startsWith(SITE_URL) ? highSchoolUrl : (SITE_URL + highSchoolUrl),
                middleSchoolUrl.startsWith(SITE_URL) ? middleSchoolUrl : (SITE_URL + middleSchoolUrl),
            );

            await updateRemoteConfig(newConfigValues);
        } catch (error) {
            console.error("Error checking for new timetable:", error);
        }
    });

/**
 * Register a listener for the Firebase Remote Config
 *
 * This function is called when the Remote Config is changed
 */
exports.sendNewTimetableNotification = functions
    .region('europe-west1')
    .remoteConfig
    .onUpdate(async (versionMetadata) => {
        const config = admin.remoteConfig(); // Get Access to Firebase Remote Config

        // Fetch both the current and the previous template concurrently in parallel
        const [newTemplate, oldTemplate] = await Promise.all([
            config.getTemplate(),
            config.getTemplateAtVersion(versionMetadata.versionNumber - 1)
        ]);

        // Parse the templates
        const newValues = processRemoteConfigTemplate(newTemplate);
        const oldValues = processRemoteConfigTemplate(oldTemplate);

        let titlePrefix = "";
        let channel = CHANNEL_ID_DEFAULT; // By default, send the notification to everyone

        if (oldValues.middleSchool === newValues.middleSchool && oldValues.highSchool !== newValues.highSchool) {
            // Only the high school link has changed
            titlePrefix = "Lyzeum: ";
            channel = CHANNEL_ID_HIGH_SCHOOL;

        } else if (oldValues.middleSchool !== newValues.middleSchool && oldValues.highSchool === newValues.highSchool) {
            // Only the middle school link has changed
            titlePrefix = "Gymnasium: ";
            channel = CHANNEL_ID_MIDDLE_SCHOOL;

        } else if (oldValues.middleSchool === newValues.middleSchool && oldValues.highSchool === newValues.highSchool) {
            return; // None of the links have changed, do not send a notification
        }

        // Get a random title and message for the notification
        const selectedTitle = TITLES[randomInt(0, TITLES.length - 1)];
        const selectedMessage = MESSAGES[randomInt(0, MESSAGES.length - 1)];

        const payload = {
            data: {
                title: titlePrefix + selectedTitle,
                body: selectedMessage,
                channel_id: channel
            },
            condition: "\'all\' in topics"
        };

        try {
            await admin.messaging().send(payload);
            console.log("Notification sent successfully");
        } catch (err) {
            console.error("Notification failed to send:", err);
        }
    });
