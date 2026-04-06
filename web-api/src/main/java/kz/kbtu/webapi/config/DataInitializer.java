package kz.kbtu.webapi.config;

import kz.kbtu.common.entity.GlossaryTerm;
import kz.kbtu.common.entity.Quiz;
import kz.kbtu.common.entity.QuizQuestion;
import kz.kbtu.webapi.repository.GlossaryTermRepository;
import kz.kbtu.webapi.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final GlossaryTermRepository glossaryTermRepository;
    private final QuizRepository quizRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initializeGlossary();
        initializeQuizzes();
    }

    private void initializeGlossary() {
        if (glossaryTermRepository.count() > 0) {
            log.info("Glossary terms already initialized");
            return;
        }

        log.info("Initializing glossary terms...");

        List<GlossaryTerm> terms = List.of(
            GlossaryTerm.builder().term("Bear Market")
                .definition("A market condition in which prices are falling or expected to fall. It typically describes a condition where securities prices fall 20% or more from recent highs.")
                .category("Market Basics").build(),
            GlossaryTerm.builder().term("Bull Market")
                .definition("A market condition where prices are rising or expected to rise. The term is typically used to refer to the stock market but can apply to anything traded, such as bonds, real estate, currencies, and commodities.")
                .category("Market Basics").build(),
            GlossaryTerm.builder().term("Dividend")
                .definition("A distribution of a portion of a company's earnings to its shareholders. Dividends are typically paid quarterly and represent a return on investment for stockholders.")
                .category("Income").build(),
            GlossaryTerm.builder().term("ETF")
                .definition("Exchange-Traded Fund. A type of pooled investment security that operates much like a mutual fund but trades on stock exchanges like individual stocks.")
                .category("Investment Vehicles").build(),
            GlossaryTerm.builder().term("Market Cap")
                .definition("Market capitalization. The total market value of a company's outstanding shares, calculated by multiplying the share price by the number of shares outstanding.")
                .category("Valuation").build(),
            GlossaryTerm.builder().term("P/E Ratio")
                .definition("Price-to-Earnings ratio. A valuation metric calculated by dividing the market price per share by earnings per share. It indicates how much investors are willing to pay per dollar of earnings.")
                .category("Valuation").build(),
            GlossaryTerm.builder().term("Short Selling")
                .definition("An investment strategy that speculates on the decline of a stock's price. Traders borrow shares to sell at the current price, hoping to buy them back later at a lower price.")
                .category("Trading").build(),
            GlossaryTerm.builder().term("Volatility")
                .definition("A statistical measure of the dispersion of returns for a given security or market index. Higher volatility means greater price fluctuations and potentially higher risk.")
                .category("Risk").build(),
            GlossaryTerm.builder().term("Liquidity")
                .definition("The ease with which an asset can be converted into cash without significantly affecting its market price. Highly liquid assets can be sold quickly at a fair price.")
                .category("Market Basics").build(),
            GlossaryTerm.builder().term("Diversification")
                .definition("A risk management strategy that mixes a wide variety of investments within a portfolio to reduce exposure to any single asset or risk.")
                .category("Risk").build(),
            GlossaryTerm.builder().term("Hedge Fund")
                .definition("A pooled investment fund that employs various strategies to generate active returns for its investors. Hedge funds may use leverage, derivatives, and short selling.")
                .category("Investment Vehicles").build(),
            GlossaryTerm.builder().term("IPO")
                .definition("Initial Public Offering. The process by which a private company first sells shares to the public, transitioning from private to publicly traded status on a stock exchange.")
                .category("Market Basics").build(),
            GlossaryTerm.builder().term("Portfolio")
                .definition("A collection of financial investments such as stocks, bonds, commodities, cash, and their fund equivalents held by an investor.")
                .category("Investment Vehicles").build(),
            GlossaryTerm.builder().term("Beta")
                .definition("A measure of a stock's volatility in relation to the overall market. A beta greater than 1 indicates higher volatility than the market; less than 1 indicates lower volatility.")
                .category("Risk").build(),
            GlossaryTerm.builder().term("Yield")
                .definition("The earnings generated and realized on an investment over a particular period, expressed as a percentage of the investment's cost or market value.")
                .category("Income").build(),
            GlossaryTerm.builder().term("Bonds")
                .definition("Fixed-income instruments representing a loan made by an investor to a borrower. Bonds pay periodic interest and return the principal at maturity.")
                .category("Investment Vehicles").build()
        );

        glossaryTermRepository.saveAll(terms);
        log.info("Initialized {} glossary terms", terms.size());
    }

    private void initializeQuizzes() {
        if (quizRepository.count() > 0) {
            log.info("Quizzes already initialized");
            return;
        }

        log.info("Initializing quizzes...");

        List<Quiz> quizzes = List.of(
            buildQuiz("Market Basics",
                "Learn the fundamentals of stock markets, including bull and bear markets, trading volume, and market cycles.",
                Quiz.Difficulty.BEGINNER,
                List.of(
                    buildQuestion("What typically indicates a bull market?",
                        List.of("Prices falling 20% from recent highs", "Rising or expected rising prices", "High trading volume", "Increased market volatility"),
                        1, "A bull market is characterized by rising prices or expectations of rising prices, typically associated with investor optimism and economic growth.", 0),
                    buildQuestion("A company with a high P/E ratio generally indicates:",
                        List.of("The company is undervalued", "Investors expect higher future growth", "The company is paying high dividends", "The stock price is declining"),
                        1, "A high P/E ratio typically suggests that investors expect higher earnings growth in the future compared to companies with lower P/E ratios.", 1),
                    buildQuestion("What is the primary purpose of short selling?",
                        List.of("To profit from rising stock prices", "To earn dividend income", "To profit from declining stock prices", "To reduce portfolio volatility"),
                        2, "Short selling is a strategy used to profit from an expected decline in a stock's price by borrowing and selling shares, then buying them back at a lower price.", 2)
                )),

            buildQuiz("Valuation Metrics",
                "Understand key valuation metrics used to analyze stocks, including P/E ratio, market cap, and dividend yield.",
                Quiz.Difficulty.BEGINNER,
                List.of(
                    buildQuestion("What does market capitalization represent?",
                        List.of("Annual revenue of a company", "Total value of outstanding shares", "Net profit margin", "Total debt of a company"),
                        1, "Market capitalization is calculated by multiplying the current share price by the total number of outstanding shares, representing the total market value of a company.", 0),
                    buildQuestion("A low P/E ratio compared to industry peers may suggest:",
                        List.of("The stock is overvalued", "The stock is potentially undervalued", "High growth expectations", "The company will go bankrupt"),
                        1, "A low P/E ratio relative to peers can indicate a stock is undervalued, though it may also reflect lower growth expectations or underlying issues.", 1),
                    buildQuestion("Dividend yield is calculated by dividing:",
                        List.of("Earnings by share price", "Annual dividend by share price", "Share price by annual dividend", "Revenue by number of shares"),
                        1, "Dividend yield is the annual dividend payment divided by the stock price, expressed as a percentage. It shows the return from dividends alone.", 2)
                )),

            buildQuiz("Trading Strategies",
                "Explore different trading strategies including day trading, swing trading, and position trading approaches.",
                Quiz.Difficulty.INTERMEDIATE,
                List.of(
                    buildQuestion("What is a stop-loss order?",
                        List.of("An order to buy at a specific price", "An order to sell when price drops to a set level", "A limit on daily trades", "An order that expires at market close"),
                        1, "A stop-loss order automatically sells a security when it reaches a specified price, limiting potential losses on a position.", 0),
                    buildQuestion("Dollar-cost averaging involves:",
                        List.of("Investing a lump sum at once", "Investing fixed amounts at regular intervals", "Only buying when prices drop", "Trading based on currency fluctuations"),
                        1, "Dollar-cost averaging means investing a fixed amount regularly regardless of price, which reduces the impact of volatility over time.", 1),
                    buildQuestion("What distinguishes swing trading from day trading?",
                        List.of("Swing trading uses more capital", "Swing trading holds positions for days to weeks", "Day trading is less risky", "Swing trading only involves options"),
                        1, "Swing trading holds positions for several days to weeks, aiming to capture short-to-medium-term price movements, while day trading closes all positions within the same day.", 2)
                )),

            buildQuiz("Risk Management",
                "Master essential risk management concepts including diversification, hedging, and portfolio allocation strategies.",
                Quiz.Difficulty.INTERMEDIATE,
                List.of(
                    buildQuestion("What is the primary benefit of portfolio diversification?",
                        List.of("Guaranteeing profits", "Eliminating all risk", "Reducing unsystematic risk", "Maximizing short-term returns"),
                        2, "Diversification reduces unsystematic (company-specific) risk by spreading investments across different assets, sectors, and geographies.", 0),
                    buildQuestion("What does beta measure in investing?",
                        List.of("A stock's dividend growth rate", "A stock's volatility relative to the market", "The total return of a portfolio", "A company's debt-to-equity ratio"),
                        1, "Beta measures a stock's volatility relative to the overall market. A beta greater than 1 indicates higher volatility than the market.", 1),
                    buildQuestion("What is a hedging strategy?",
                        List.of("Buying only growth stocks", "Taking an offsetting position to reduce risk", "Selling all positions before earnings", "Investing only in bonds"),
                        1, "Hedging involves taking an offsetting investment position to reduce the risk of adverse price movements in an existing position.", 2)
                )),

            buildQuiz("Technical Analysis",
                "Dive into technical analysis tools like moving averages, RSI, support and resistance levels.",
                Quiz.Difficulty.ADVANCED,
                List.of(
                    buildQuestion("What does a \"golden cross\" pattern indicate?",
                        List.of("A bearish reversal", "A bullish signal when short-term MA crosses above long-term MA", "High trading volume", "A stock reaching its all-time high"),
                        1, "A golden cross occurs when a short-term moving average crosses above a long-term moving average, typically seen as a bullish signal.", 0),
                    buildQuestion("An RSI value above 70 generally indicates:",
                        List.of("The asset is oversold", "The asset is overbought", "Normal trading conditions", "Low volatility"),
                        1, "An RSI (Relative Strength Index) above 70 suggests an asset may be overbought and could be due for a price correction or pullback.", 1),
                    buildQuestion("What is a support level?",
                        List.of("The highest price a stock has reached", "A price level where buying pressure tends to prevent further decline", "The average trading price", "A government-mandated minimum price"),
                        1, "A support level is a price point where a stock tends to stop falling because buying interest increases, creating a floor for the price.", 2)
                )),

            buildQuiz("Global Markets",
                "Understand how international markets interact, currency effects, and geopolitical factors influencing investments.",
                Quiz.Difficulty.ADVANCED,
                List.of(
                    buildQuestion("How does a strengthening US dollar typically affect US exporters?",
                        List.of("Increases their competitiveness abroad", "Makes their products more expensive overseas", "Has no effect on exports", "Reduces import costs only"),
                        1, "A stronger US dollar makes American goods more expensive for foreign buyers, potentially reducing demand for US exports and hurting exporter revenues.", 0),
                    buildQuestion("What are emerging markets?",
                        List.of("Markets that only trade technology stocks", "Economies transitioning toward more advanced financial systems", "Markets that operate only during certain hours", "Stock exchanges less than 5 years old"),
                        1, "Emerging markets are economies in the process of rapid growth and industrialization, offering higher growth potential but also carrying more risk than developed markets.", 1),
                    buildQuestion("What is geopolitical risk in investing?",
                        List.of("Risk from natural disasters only", "Risk from political events and tensions affecting markets", "Risk of technology failures", "Risk from interest rate changes"),
                        1, "Geopolitical risk refers to the potential for political events, conflicts, or policy changes between nations to negatively impact investment returns.", 2)
                ))
        );

        quizRepository.saveAll(quizzes);
        log.info("Initialized {} quizzes", quizzes.size());
    }

    private Quiz buildQuiz(String title, String description, Quiz.Difficulty difficulty, List<QuizQuestion> questions) {
        Quiz quiz = Quiz.builder()
                .title(title)
                .description(description)
                .difficulty(difficulty)
                .build();
        for (QuizQuestion q : questions) {
            q.setQuiz(quiz);
        }
        quiz.getQuestions().addAll(questions);
        return quiz;
    }

    private QuizQuestion buildQuestion(String question, List<String> options, int correctAnswer, String explanation, int order) {
        return QuizQuestion.builder()
                .question(question)
                .options(options)
                .correctAnswer(correctAnswer)
                .explanation(explanation)
                .questionOrder(order)
                .build();
    }
}
