import logging
import os

import httpx
import asyncio
from dotenv import load_dotenv
from telegram import Update
from telegram.ext import ApplicationBuilder, CommandHandler, ContextTypes

load_dotenv()

BOT_TOKEN  = os.environ["BOT_TOKEN"]
API_URL    = os.environ["API_URL"]        # e.g. http://localhost:8080
BOT_SECRET = os.environ["BOT_SECRET"]

logging.basicConfig(
    format="%(asctime)s [%(levelname)s] %(message)s",
    level=logging.INFO,
)
log = logging.getLogger(__name__)


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    if not context.args:
        await update.message.reply_text(
            "Use the <b>Connect Telegram</b> button on the website to link your account.",
            parse_mode="HTML",
        )
        return

    token    = context.args[0]
    chat_id  = update.effective_chat.id
    username = update.effective_user.username or str(chat_id)

    try:
        r = httpx.post(
            f"{API_URL}/api/telegram/callback",
            json={"token": token, "chatId": chat_id, "telegramUsername": username},
            headers={"X-Bot-Secret": BOT_SECRET},
            timeout=10,
        )

        if r.status_code == 200:
            await update.message.reply_text(
                "✅ Your account has been successfully linked!\n"
                "You will now receive news notifications here.",
            )
        elif r.status_code == 400:
            await update.message.reply_text(
                "❌ This link has expired or is invalid.\n"
                "Please request a new one from the website.",
            )
        elif r.status_code == 403:
            log.error("Bot secret rejected by server — check BOT_SECRET env var.")
            await update.message.reply_text("Server configuration error. Please contact support.")
        else:
            log.error("Unexpected status %s: %s", r.status_code, r.text)
            await update.message.reply_text("Something went wrong. Please try again.")

    except httpx.RequestError as e:
        log.error("Could not reach API: %s", e)
        await update.message.reply_text("Could not reach the server. Please try again later.")


if __name__ == "__main__":
    app = ApplicationBuilder().token(BOT_TOKEN).build()
    app.add_handler(CommandHandler("start", start))
    log.info("Bot is running...")
    asyncio.set_event_loop(asyncio.new_event_loop())
    app.run_polling()
