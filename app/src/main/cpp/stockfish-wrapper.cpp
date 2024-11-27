// stockfish-wrapper.cpp
#include <jni.h>
#include <string>
#include "position.h"
#include "uci.h"
#include "search.h"
#include "thread.h"
#include "types.h"
#include "movegen.h"
#include "movepick.h"
#include "xboard.h"

using namespace Stockfish;
using namespace XBoard;

// Global engine state
static Position* pos = nullptr;
static StateListPtr states;
static Thread* mainThread = nullptr;
static XBoard::StateMachine* xboardStateMachine = nullptr;

// Helper function to convert a PieceType to a String
std::string piece_to_string(PieceType type) {
    switch (type) {
        case PAWN: return "PAWN";
        case KNIGHT: return "KNIGHT";
        case BISHOP: return "BISHOP";
        case ROOK: return "ROOK";
        case QUEEN: return "QUEEN";
        case KING: return "KING";
        case FERS: return "FERS";
        case ALFIL: return "ALFIL";
        case SILVER: return "SILVER";
        case BERS: return "BERS";
        case ARCHBISHOP: return "ARCHBISHOP";
        case CHANCELLOR: return "CHANCELLOR";
        case AMAZON: return "AMAZON";
        case SHOGI_PAWN: return "SHOGI_PAWN";
        case LANCE: return "LANCE";
        case SHOGI_KNIGHT: return "SHOGI_KNIGHT";
        case GOLD: return "GOLD";
        case DRAGON_HORSE: return "DRAGON_HORSE";
        case CANNON: return "CANNON";
        case JANGGI_CANNON: return "JANGGI_CANNON";
        case SOLDIER: return "SOLDIER";
        case HORSE: return "HORSE";
        case ELEPHANT: return "ELEPHANT";
        case JANGGI_ELEPHANT: return "JANGGI_ELEPHANT";
        case BANNER: return "BANNER";
        case WAZIR: return "WAZIR";
        case COMMONER: return "COMMONER";
        case CENTAUR: return "CENTAUR";
        default: return "UNKNOWN";
    }
}

extern "C" {

JNIEXPORT void JNICALL
Java_emerald_apps_fairychess_model_board_Chessboard_initializeEngine(JNIEnv*, jobject) {
    UCI::init(Options);
    Threads.set(1);
    mainThread = Threads.main();

    states = StateListPtr(new std::deque<StateInfo>(1));
}

JNIEXPORT jstring JNICALL
Java_emerald_apps_fairychess_model_board_Chessboard_getAIMove(JNIEnv* env, jobject, jstring colorStr) {
    const char* color = env->GetStringUTFChars(colorStr, 0);

    // Set the position
    std::string fen = pos->fen();
    pos->set(variants.find("chess")->second, fen, color[0] == 'W', &states->back(), mainThread, false);

    // Initialize XBoard state machine
    xboardStateMachine = new StateMachine(*pos, states);

    // Start the search using the XBoard API
    Search::LimitsType searchLimits;
    searchLimits.depth = 20;
    xboardStateMachine->go(searchLimits, false);

    // Wait for the search to finish and get the best move
    xboardStateMachine->stop(false);
    Move bestMove = MOVE_NONE;
    if (xboardStateMachine->moveAfterSearch) {
        bestMove = xboardStateMachine->ponderMove;
        xboardStateMachine->moveAfterSearch = false;
    }

    // Convert the best move to a UCI string
    std::string bestMoveStr = UCI::move(*pos, bestMove);
    env->ReleaseStringUTFChars(colorStr, color);
    return env->NewStringUTF(bestMoveStr.c_str());
}

JNIEXPORT void JNICALL
Java_emerald_apps_fairychess_model_board_Chessboard_setPosition(JNIEnv* env, jobject, jstring fen) {
    const char* nativeFen = env->GetStringUTFChars(fen, 0);
    std::string fenStr(nativeFen);
    pos->set(variants.find("chess")->second, fenStr, false, &states->back(), mainThread, false);
    env->ReleaseStringUTFChars(fen, nativeFen);
}

JNIEXPORT jstring JNICALL
Java_emerald_apps_fairychess_model_board_Chessboard_makeMove(JNIEnv* env, jobject, jstring moveStr) {
    const char* moveUCI = env->GetStringUTFChars(moveStr, 0);
    std::string str(moveUCI);

    Move m = UCI::to_move(*pos, str);

    if (m != MOVE_NONE && pos->legal(m)) {
        states->emplace_back();
        pos->do_move(m, states->back());

        // Get the new position as FEN
        std::string newFen = pos->fen();
        env->ReleaseStringUTFChars(moveStr, moveUCI);
        return env->NewStringUTF(newFen.c_str());
    }

    env->ReleaseStringUTFChars(moveStr, moveUCI);
    return env->NewStringUTF(""); // Return empty string for illegal move
}

JNIEXPORT jobjectArray JNICALL
Java_emerald_apps_fairychess_model_board_Chessboard_getLegalMoves(JNIEnv* env, jobject, jstring squareStr) {
    const char* sq = env->GetStringUTFChars(squareStr, 0);

    // Convert square string (e.g. "e2") to internal square
    Square from = make_square(File(sq[0] - 'a'), Rank(sq[1] - '1'));

    std::vector<std::string> legal_moves;

    // Generate all legal moves
    MoveList<LEGAL> moves(*pos);

    // Filter moves for the given square
    for (const auto& m : moves) {
        if (from_sq(m) == from) {
            legal_moves.push_back(UCI::move(*pos, m));
        }
    }

    // Create Java string array
    jobjectArray result = env->NewObjectArray(legal_moves.size(),
                                              env->FindClass("java/lang/String"),
                                              env->NewStringUTF(""));

    // Fill the array
    for (size_t i = 0; i < legal_moves.size(); i++) {
        env->SetObjectArrayElement(result, i,
                                   env->NewStringUTF(legal_moves[i].c_str()));
    }

    env->ReleaseStringUTFChars(squareStr, sq);
    return result;
}

JNIEXPORT jstring JNICALL
Java_emerald_apps_fairychess_model_board_Chessboard_getCurrentFen(JNIEnv* env, jobject) {
    std::string fen = pos->fen();
    return env->NewStringUTF(fen.c_str());
}

JNIEXPORT jint JNICALL
Java_emerald_apps_fairychess_model_board_Chessboard_getGameResult(JNIEnv*, jobject) {
    // Check for checkmate
    if (pos->checkers()) {
        if (MoveList<LEGAL>(*pos).size() == 0) {
            // If side to move is in check and has no legal moves, it's checkmate
            return pos->side_to_move() == WHITE ? -1 : 1;  // If white to move, black wins (-1) and vice versa
        }
    }

    // Check for stalemate (no legal moves but not in check)
    if (MoveList<LEGAL>(*pos).size() == 0) {
        return 0;  // Draw
    }

    // Check other draw conditions (insufficient material, fifty-move rule, etc.)
    if (pos->is_draw(50)) {
        return 0;  // Draw
    }

    return 2;  // Game is ongoing
}

JNIEXPORT jobject JNICALL
Java_emerald_apps_fairychess_model_board_Chessboard_getPiece(JNIEnv* env, jobject, jstring squareStr) {
    const char* sq = env->GetStringUTFChars(squareStr, 0);

    // Convert square string (e.g. "e2") to internal square
    Square square = make_square(File(sq[0] - 'a'), Rank(sq[1] - '1'));

    // Get the piece type
    Piece piece = pos->piece_on(square);
    PieceType pieceType = type_of(piece);

    // Determine the color based on the piece type
    const char* color = (piece & 1) == WHITE ? "WHITE" : "BLACK";

    jclass pairClass = env->FindClass("java/util/AbstractMap$SimpleEntry");
    jmethodID pairCtor = env->GetMethodID(pairClass, "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V");

    // Create a new Pair object to return the piece type and color
    jobject pairObj = env->NewObject(pairClass, pairCtor,
                                     env->NewStringUTF(piece_to_string(pieceType).c_str()),
                                     env->NewStringUTF(color));

    env->ReleaseStringUTFChars(squareStr, sq);
    return pairObj;
}

// Cleanup
JNIEXPORT void JNICALL
Java_emerald_apps_fairychess_model_board_Chessboard_quit(JNIEnv*, jobject) {
    if (pos != nullptr) {
        delete pos;
        pos = nullptr;
    }
    Threads.set(0);
}

}