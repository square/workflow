/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sample.poetry.model

import com.squareup.sample.poetry.model.Poet.Poe

val Raven = Poem(
    title = "The Raven",
    poet = Poe,

    stanzas = listOf(
        listOf(
            "Once upon a midnight dreary, while I pondered, weak and weary,",
            "Over many a quaint and curious volume of forgotten lore—",
            "\tWhile I nodded, nearly napping, suddenly there came a tapping,",
            "As of some one gently rapping, rapping at my chamber door.",
            "“’Tis some visitor,” I muttered, “tapping at my chamber door—",
            "\t\t\tOnly this and nothing more.”"
        ),

        listOf(
            "\tAh, distinctly I remember it was in the bleak December;",
            "And each separate dying ember wrought its ghost upon the floor.",
            "\tEagerly I wished the morrow;—vainly I had sought to borrow",
            "\tFrom my books surcease of sorrow—sorrow for the lost Lenore—",
            "For the rare and radiant maiden whom the angels name Lenore—",
            "\t\t\tNameless here for evermore."
        ),

        listOf(
            "\tAnd the silken, sad, uncertain rustling of each purple curtain",
            "Thrilled me—filled me with fantastic terrors never felt before;",
            "\tSo that now, to still the beating of my heart, I stood repeating",
            "\t“’Tis some visitor entreating entrance at my chamber door—",
            "Some late visitor entreating entrance at my chamber door;—",
            "\t\t\tThis it is and nothing more.”"
        ),

        listOf(
            "\tPresently my soul grew stronger; hesitating then no longer,",
            "“Sir,” said I, “or Madam, truly your forgiveness I implore;",
            "\tBut the fact is I was napping, and so gently you came rapping,",
            "\tAnd so faintly you came tapping, tapping at my chamber door,",
            "That I scarce was sure I heard you”—here I opened wide the door;—",
            "\t\t\tDarkness there and nothing more."
        ),

        listOf(
            "\tDeep into that darkness peering, long I stood there wondering, fearing,",
            "Doubting, dreaming dreams no mortal ever dared to dream before;",
            "\tBut the silence was unbroken, and the stillness gave no token,",
            "\tAnd the only word there spoken was the whispered word, “Lenore?”",
            "This I whispered, and an echo murmured back the word, “Lenore!”—",
            "\t\t\tMerely this and nothing more."
        ),

        listOf(
            "\tBack into the chamber turning, all my soul within me burning,",
            "Soon again I heard a tapping somewhat louder than before.",
            "\t“Surely,” said I, “surely that is something at my window lattice;",
            "\t  Let me see, then, what thereat is, and this mystery explore—",
            "Let my heart be still a moment and this mystery explore;—",
            "\t\t\t’Tis the wind and nothing more!”"
        ),

        listOf(
            "\tOpen here I flung the shutter, when, with many a flirt and flutter,",
            "In there stepped a stately Raven of the saintly days of yore;",
            "\tNot the least obeisance made he; not a minute stopped or stayed he;",
            "\tBut, with mien of lord or lady, perched above my chamber door—",
            "Perched upon a bust of Pallas just above my chamber door—",
            "\t\t\tPerched, and sat, and nothing more."
        ),

        listOf(
            "Then this ebony bird beguiling my sad fancy into smiling,",
            "By the grave and stern decorum of the countenance it wore,",
            "“Though thy crest be shorn and shaven, thou,” I said, “art sure no craven,",
            "Ghastly grim and ancient Raven wandering from the Nightly shore—",
            "Tell me what thy lordly name is on the Night’s Plutonian shore!”",
            "\t\t\tQuoth the Raven “Nevermore.”"
        ),

        listOf(
            "\tMuch I marvelled this ungainly fowl to hear discourse so plainly,",
            "Though its answer little meaning—little relevancy bore;",
            "\tFor we cannot help agreeing that no living human being",
            "\tEver yet was blessed with seeing bird above his chamber door—",
            "Bird or beast upon the sculptured bust above his chamber door,",
            "\t\t\tWith such name as “Nevermore.”"
        ),

        listOf(
            "\tBut the Raven, sitting lonely on the placid bust, spoke only",
            "That one word, as if his soul in that one word he did outpour.",
            "\tNothing farther then he uttered—not a feather then he fluttered—",
            "\tTill I scarcely more than muttered “Other friends have flown before—",
            "On the morrow he will leave me, as my Hopes have flown before.”",
            "\t\t\tThen the bird said “Nevermore.”"
        ),

        listOf(
            "\tStartled at the stillness broken by reply so aptly spoken,",
            "“Doubtless,” said I, “what it utters is its only stock and store",
            "\tCaught from some unhappy master whom unmerciful Disaster",
            "\tFollowed fast and followed faster till his songs one burden bore—",
            "Till the dirges of his Hope that melancholy burden bore",
            "\t\t\tOf ‘Never—nevermore’.”"
        ),

        listOf(
            "\tBut the Raven still beguiling all my fancy into smiling,",
            "Straight I wheeled a cushioned seat in front of bird, and bust and door;",
            "\tThen, upon the velvet sinking, I betook myself to linking",
            "\tFancy unto fancy, thinking what this ominous bird of yore—",
            "What this grim, ungainly, ghastly, gaunt, and ominous bird of yore",
            "\t\t\tMeant in croaking “Nevermore.”"
        ),

        listOf(
            "\tThis I sat engaged in guessing, but no syllable expressing",
            "To the fowl whose fiery eyes now burned into my bosom’s core;",
            "\tThis and more I sat divining, with my head at ease reclining",
            "\tOn the cushion’s velvet lining that the lamp-light gloated o’er,",
            "But whose velvet-violet lining with the lamp-light gloating o’er,",
            "\t\t\tShe shall press, ah, nevermore!"
        ),

        listOf(
            "\tThen, methought, the air grew denser, perfumed from an unseen censer",
            "Swung by Seraphim whose foot-falls tinkled on the tufted floor.",
            "\t“Wretch,” I cried, “thy God hath lent thee—by these angels he hath sent thee",
            "\tRespite—respite and nepenthe from thy memories of Lenore;",
            "Quaff, oh quaff this kind nepenthe and forget this lost Lenore!”",
            "\t\t\tQuoth the Raven “Nevermore.”"
        ),

        listOf(
            "\t“Prophet!” said I, “thing of evil!—prophet still, if bird or devil!—",
            "Whether Tempter sent, or whether tempest tossed thee here ashore,",
            "\tDesolate yet all undaunted, on this desert land enchanted—",
            "\tOn this home by Horror haunted—tell me truly, I implore—",
            "Is there—is there balm in Gilead?—tell me—tell me, I implore!”",
            "\t\t\tQuoth the Raven “Nevermore.”"
        ),

        listOf(
            "\t“Prophet!” said I, “thing of evil!—prophet still, if bird or devil!",
            "By that Heaven that bends above us—by that God we both adore—",
            "\tTell this soul with sorrow laden if, within the distant Aidenn,",
            "\tIt shall clasp a sainted maiden whom the angels name Lenore—",
            "Clasp a rare and radiant maiden whom the angels name Lenore.”",
            "\t\t\tQuoth the Raven “Nevermore.”"
        ),

        listOf(
            "\t“Be that word our sign of parting, bird or fiend!” I shrieked, upstarting—",
            "“Get thee back into the tempest and the Night’s Plutonian shore!",
            "\tLeave no black plume as a token of that lie thy soul hath spoken!",
            "\tLeave my loneliness unbroken!—quit the bust above my door!",
            "Take thy beak from out my heart, and take thy form from off my door!”",
            "\t\t\tQuoth the Raven “Nevermore.”"
        ),

        listOf(
            "\tAnd the Raven, never flitting, still is sitting, still is sitting",
            "On the pallid bust of Pallas just above my chamber door;",
            "\tAnd his eyes have all the seeming of a demon’s that is dreaming,",
            "\tAnd the lamp-light o’er him streaming throws his shadow on the floor;",
            "And my soul from out that shadow that lies floating on the floor",
            "\t\t\tShall be lifted—nevermore!"
        )
    )
)
